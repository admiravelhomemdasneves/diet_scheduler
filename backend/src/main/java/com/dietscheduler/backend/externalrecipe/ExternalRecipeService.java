package com.dietscheduler.backend.externalrecipe;

import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.config.HttpClientProperties;
import com.dietscheduler.backend.externalrecipe.dto.ExternalRecipeDetail;
import com.dietscheduler.backend.externalrecipe.dto.ExternalRecipeIngredient;
import com.dietscheduler.backend.pantry.OpenFoodFactsClient;
import com.dietscheduler.backend.preferences.TasteType;
import com.dietscheduler.backend.recipe.NutritionEstimator;
import com.dietscheduler.backend.recipe.Recipe;
import com.dietscheduler.backend.recipe.RecipeCategory;
import com.dietscheduler.backend.recipe.RecipeRepository;
import com.dietscheduler.backend.recipe.RecipeService;
import com.dietscheduler.backend.recipe.dto.CreateRecipeRequest;
import com.dietscheduler.backend.recipe.dto.RecipeIngredientRequest;
import com.dietscheduler.backend.recipe.dto.RecipeResponse;
import com.dietscheduler.backend.recipe.dto.RecipeTagRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks up and imports recipes found via {@link TheMealDbClient} into our own Recipe model.
 * Mapping is intentionally best-effort for now (see class-level TODO areas below) --
 * servings, prep/cook time and nutrition aren't reliably available from the source API,
 * and measure strings ("1 cup", "to taste", "3") are freeform, not structured quantity+unit.
 * A deeper, more faithful mapping (matching ingredient/tag taxonomies precisely) is planned later.
 */
@Service
public class ExternalRecipeService {

    private static final Pattern LEADING_NUMBER =
            Pattern.compile("^(\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:\\.\\d+)?)\\s*");
    private static final int DEFAULT_SERVINGS = 4;

    private final TheMealDbClient client;
    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final Executor externalApiExecutor;
    private final long perLookupTimeoutMs;

    public ExternalRecipeService(TheMealDbClient client, RecipeService recipeService, RecipeRepository recipeRepository,
                                  OpenFoodFactsClient openFoodFactsClient,
                                  @Qualifier("externalApiExecutor") Executor externalApiExecutor,
                                  HttpClientProperties httpClientProperties) {
        this.client = client;
        this.recipeService = recipeService;
        this.recipeRepository = recipeRepository;
        this.openFoodFactsClient = openFoodFactsClient;
        this.externalApiExecutor = externalApiExecutor;
        // A little slack on top of the HTTP client's own read timeout so a call that's about to
        // legitimately finish isn't cut off by this timeout first.
        this.perLookupTimeoutMs = httpClientProperties.connectTimeoutMs() + httpClientProperties.readTimeoutMs() + 1000L;
    }

    /** Live preview of an external recipe -- nothing is saved. */
    public ExternalRecipeDetail getDetail(String externalId) {
        TheMealDbClient.Meal meal = lookupOrThrow(externalId);
        List<ParsedIngredient> parsed = meal.ingredientMeasures().stream()
                .map(im -> new ParsedIngredient(im.name(), parseMeasure(im.measure())))
                .toList();
        List<ExternalRecipeIngredient> ingredients = parsed.stream()
                .map(pi -> new ExternalRecipeIngredient(pi.name(), pi.measure().quantity(), pi.measure().unit()))
                .toList();

        // One Open Food Facts search per ingredient, run concurrently -- sequentially this would
        // be several seconds for a typical 8-15-ingredient recipe. Nothing is persisted; this is
        // purely to estimate nutrition for a recipe the household hasn't favorited (yet). Runs on
        // a small bounded pool (not the JVM-wide common ForkJoinPool, which blocking HTTP calls
        // here would otherwise starve for every other CompletableFuture/parallel stream in the
        // process), and each lookup individually times out and degrades to "no nutrition data for
        // this ingredient" rather than one slow upstream call stalling the whole preview.
        List<CompletableFuture<NutritionEstimator.IngredientNutrition>> futures = parsed.stream()
                .map(pi -> CompletableFuture.supplyAsync(() -> lookupNutrition(pi), externalApiExecutor)
                        .orTimeout(perLookupTimeoutMs, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> emptyNutrition(pi)))
                .toList();
        List<NutritionEstimator.IngredientNutrition> nutritionItems = futures.stream().map(CompletableFuture::join).toList();
        RecipeResponse.NutritionEstimate estimate = NutritionEstimator.estimate(nutritionItems, DEFAULT_SERVINGS);

        return new ExternalRecipeDetail(externalId, nameOf(meal), guessCategory(meal.category), meal.instructions,
                DEFAULT_SERVINGS, meal.thumbnailUrl, ingredients, buildTags(meal),
                estimate != null ? estimate.calories() : null,
                estimate != null ? estimate.protein() : null,
                estimate != null ? estimate.carbs() : null,
                estimate != null ? estimate.fat() : null,
                estimate != null && estimate.incomplete());
    }

    /** Best (first) Open Food Facts match for this ingredient's free-text name, since TheMealDB gives no ingredient id to look up directly. */
    private NutritionEstimator.IngredientNutrition lookupNutrition(ParsedIngredient pi) {
        List<OpenFoodFactsClient.Product> results = openFoodFactsClient.search(pi.name());
        if (results.isEmpty()) {
            return emptyNutrition(pi);
        }
        OpenFoodFactsClient.Nutriments n = results.get(0).nutriments();
        return new NutritionEstimator.IngredientNutrition(pi.measure().quantity(), pi.measure().unit(),
                n != null ? n.energyKcal100g() : null, n != null ? n.proteins100g() : null,
                n != null ? n.carbohydrates100g() : null, n != null ? n.fat100g() : null);
    }

    /** Same "no data for this ingredient" shape used both when Open Food Facts has no match and
     * when the lookup times out -- NutritionEstimator already treats a null-nutrition entry as
     * "skip this ingredient, flag the estimate incomplete" rather than a hard failure. */
    private NutritionEstimator.IngredientNutrition emptyNutrition(ParsedIngredient pi) {
        return new NutritionEstimator.IngredientNutrition(pi.measure().quantity(), pi.measure().unit(), null, null, null, null);
    }

    private record ParsedIngredient(String name, ParsedMeasure measure) {
    }

    @Transactional
    public RecipeResponse importAsFavorite(UUID userId, String externalId) {
        TheMealDbClient.Meal meal = lookupOrThrow(externalId);
        String name = nameOf(meal);

        Recipe existing = recipeRepository.findFirstByNameIgnoreCaseAndCreatedByUserId(name, userId).orElse(null);
        if (existing != null) {
            return recipeService.getRecipe(existing.getId(), userId);
        }

        return recipeService.createRecipe(userId, toCreateRequest(meal, name));
    }

    private TheMealDbClient.Meal lookupOrThrow(String externalId) {
        return client.lookup(externalId).orElseThrow(() -> new NotFoundException("External recipe not found"));
    }

    private String nameOf(TheMealDbClient.Meal meal) {
        return StringUtils.hasText(meal.name) ? meal.name.trim() : "Imported recipe";
    }

    private CreateRecipeRequest toCreateRequest(TheMealDbClient.Meal meal, String name) {
        List<RecipeIngredientRequest> ingredients = meal.ingredientMeasures().stream()
                .map(im -> {
                    ParsedMeasure parsed = parseMeasure(im.measure());
                    return new RecipeIngredientRequest(null, im.name(), parsed.quantity(), parsed.unit());
                })
                .toList();

        return new CreateRecipeRequest(
                name,
                guessCategory(meal.category),
                meal.instructions,
                DEFAULT_SERVINGS,
                null, null,
                null, null, null, null,
                meal.thumbnailUrl,
                false,
                ingredients,
                buildTags(meal)
        );
    }

    private List<RecipeTagRequest> buildTags(TheMealDbClient.Meal meal) {
        List<RecipeTagRequest> tags = new ArrayList<>();
        if (StringUtils.hasText(meal.area)) {
            tags.add(new RecipeTagRequest(TasteType.CUISINE, meal.area.trim()));
        }
        return tags;
    }

    /** TheMealDB's categories (Beef, Pasta, Vegan, Side, ...) don't line up with our 4-value enum -- rough keyword guess. */
    private RecipeCategory guessCategory(String rawCategory) {
        if (rawCategory == null) return RecipeCategory.MAIN;
        String c = rawCategory.toLowerCase();
        if (c.contains("dessert")) return RecipeCategory.DESSERT;
        if (c.contains("starter") || c.contains("side")) return RecipeCategory.SNACK;
        return RecipeCategory.MAIN;
    }

    /** Best-effort split of a freeform measure string ("1 1/2 cups", "to taste", "3") into quantity + unit. */
    private ParsedMeasure parseMeasure(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new ParsedMeasure(BigDecimal.ONE, "unit");
        }
        String trimmed = raw.trim();
        Matcher m = LEADING_NUMBER.matcher(trimmed);
        if (m.find()) {
            BigDecimal quantity = parseNumberToken(m.group(1).trim());
            String rest = trimmed.substring(m.end()).trim();
            if (quantity.signum() <= 0) quantity = BigDecimal.ONE;
            return new ParsedMeasure(quantity, rest.isEmpty() ? "unit" : rest);
        }
        return new ParsedMeasure(BigDecimal.ONE, trimmed);
    }

    /** Handles "1", "1.5", "1/2", and mixed "1 1/2". */
    private BigDecimal parseNumberToken(String token) {
        try {
            if (token.contains(" ")) {
                String[] parts = token.split("\\s+", 2);
                return new BigDecimal(parts[0]).add(parseFraction(parts[1]));
            }
            if (token.contains("/")) {
                return parseFraction(token);
            }
            return new BigDecimal(token);
        } catch (NumberFormatException | ArithmeticException e) {
            return BigDecimal.ONE;
        }
    }

    private BigDecimal parseFraction(String fraction) {
        String[] parts = fraction.split("/", 2);
        BigDecimal numerator = new BigDecimal(parts[0]);
        BigDecimal denominator = new BigDecimal(parts[1]);
        return numerator.divide(denominator, 3, java.math.RoundingMode.HALF_UP);
    }

    private record ParsedMeasure(BigDecimal quantity, String unit) {
    }
}
