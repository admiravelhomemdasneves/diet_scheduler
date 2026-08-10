package com.dietscheduler.backend.recipe;

import com.dietscheduler.backend.common.ConflictException;
import com.dietscheduler.backend.common.ForbiddenException;
import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.common.RepositoryUtils;
import com.dietscheduler.backend.config.ImagesProperties;
import com.dietscheduler.backend.pantry.Ingredient;
import com.dietscheduler.backend.pantry.IngredientRepository;
import com.dietscheduler.backend.pantry.IngredientService;
import com.dietscheduler.backend.pantry.PantryItem;
import com.dietscheduler.backend.pantry.PantryItemRepository;
import com.dietscheduler.backend.preferences.*;
import com.dietscheduler.backend.recipe.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeTagRepository recipeTagRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientService ingredientService;
    private final AllergyRepository allergyRepository;
    private final HouseholdTasteRepository householdTasteRepository;
    private final HouseholdAllergyRepository householdAllergyRepository;
    private final TasteRepository tasteRepository;
    private final PantryItemRepository pantryItemRepository;
    private final PreferenceService preferenceService;
    private final ImagesProperties imagesProperties;

    public RecipeService(RecipeRepository recipeRepository, RecipeIngredientRepository recipeIngredientRepository,
                          RecipeTagRepository recipeTagRepository, IngredientRepository ingredientRepository,
                          IngredientService ingredientService, AllergyRepository allergyRepository,
                          HouseholdTasteRepository householdTasteRepository, HouseholdAllergyRepository householdAllergyRepository,
                          TasteRepository tasteRepository, PantryItemRepository pantryItemRepository,
                          PreferenceService preferenceService, ImagesProperties imagesProperties) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeTagRepository = recipeTagRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientService = ingredientService;
        this.allergyRepository = allergyRepository;
        this.householdTasteRepository = householdTasteRepository;
        this.householdAllergyRepository = householdAllergyRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.tasteRepository = tasteRepository;
        this.preferenceService = preferenceService;
        this.imagesProperties = imagesProperties;
    }

    @Transactional
    public RecipeResponse createRecipe(UUID userId, CreateRecipeRequest request) {
        Recipe recipe = Recipe.builder()
                .name(request.name())
                .category(request.category())
                .instructions(request.instructions())
                .servings(request.servings())
                .prepTimeMinutes(request.prepTimeMinutes())
                .cookTimeMinutes(request.cookTimeMinutes())
                .caloriesPerServing(request.caloriesPerServing())
                .proteinPerServing(request.proteinPerServing())
                .carbsPerServing(request.carbsPerServing())
                .fatPerServing(request.fatPerServing())
                .createdByUserId(userId)
                .imageUrl(request.imageUrl())
                .isPrivate(request.isPrivate())
                .build();
        recipe = recipeRepository.save(recipe);

        List<RecipeIngredient> ingredients = saveIngredients(recipe.getId(), request.ingredients());
        List<RecipeTag> tags = saveTags(recipe.getId(), request.tags());

        return toResponse(recipe, ingredients, tags);
    }

    public RecipeResponse getRecipe(UUID recipeId, UUID requesterUserId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        if (recipe.isPrivate() && !Objects.equals(recipe.getCreatedByUserId(), requesterUserId)) {
            throw new ForbiddenException("This recipe is private");
        }
        return toResponse(recipe, recipeIngredientRepository.findByRecipeId(recipeId), recipeTagRepository.findByRecipeId(recipeId));
    }

    @Transactional
    public RecipeResponse updateRecipe(UUID recipeId, UUID requesterUserId, CreateRecipeRequest request) {
        Recipe recipe = requireOwnedRecipe(recipeId, requesterUserId);

        recipe.setName(request.name());
        recipe.setCategory(request.category());
        recipe.setInstructions(request.instructions());
        recipe.setServings(request.servings());
        recipe.setPrepTimeMinutes(request.prepTimeMinutes());
        recipe.setCookTimeMinutes(request.cookTimeMinutes());
        recipe.setCaloriesPerServing(request.caloriesPerServing());
        recipe.setProteinPerServing(request.proteinPerServing());
        recipe.setCarbsPerServing(request.carbsPerServing());
        recipe.setFatPerServing(request.fatPerServing());
        // Photos are managed separately via updateImage(); don't let a text-field edit silently wipe one out.
        if (request.imageUrl() != null) recipe.setImageUrl(request.imageUrl());
        recipe.setPrivate(request.isPrivate());
        recipe = recipeRepository.save(recipe);

        recipeIngredientRepository.deleteByRecipeId(recipeId);
        recipeTagRepository.deleteByRecipeId(recipeId);
        List<RecipeIngredient> ingredients = saveIngredients(recipeId, request.ingredients());
        List<RecipeTag> tags = saveTags(recipeId, request.tags());

        return toResponse(recipe, ingredients, tags);
    }

    @Transactional
    public void deleteRecipe(UUID recipeId, UUID requesterUserId) {
        requireOwnedRecipe(recipeId, requesterUserId);
        recipeIngredientRepository.deleteByRecipeId(recipeId);
        recipeTagRepository.deleteByRecipeId(recipeId);
        recipeRepository.deleteById(recipeId);
        // Previously left the file behind forever, still publicly fetchable at its old URL by
        // anyone who'd seen it -- the recipe row being gone didn't stop /recipe-images/** (permitAll,
        // static) from continuing to serve it.
        deleteImageFilesIfAny(recipeId);
    }

    // Ordered least-to-most-specific-length-signature so the shortest check that CAN determine a
    // mismatch runs first; doesn't actually matter for correctness, only which mismatch you see
    // first in a debugger.
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final Path IMAGE_DIR = Path.of("recipe-images");
    private static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "png", "webp");

    @Transactional
    public RecipeResponse updateImage(UUID recipeId, UUID requesterUserId, MultipartFile file) {
        Recipe recipe = requireOwnedRecipe(recipeId, requesterUserId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No image file provided");
        }
        if (file.getSize() > imagesProperties.maxBytes()) {
            throw new IllegalArgumentException("Image exceeds the " + (imagesProperties.maxBytes() / (1024 * 1024)) + " MB limit");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded image", e);
        }
        // Sniffed from the file's actual leading bytes, not file.getContentType() -- that header is
        // entirely client-supplied and trusting it let arbitrary bytes (with a forced .jpg/.png/
        // .webp extension) be stored and served back to anyone with the URL.
        String extension = sniffImageExtension(bytes);
        if (extension == null) {
            throw new IllegalArgumentException("File content doesn't look like a JPEG, PNG, or WEBP image");
        }

        long existingBytes = existingImageBytes(recipeId);
        long usageExcludingThisRecipe = currentImageUsageBytes(requesterUserId) - existingBytes;
        if (usageExcludingThisRecipe + bytes.length > imagesProperties.perUserQuotaBytes()) {
            throw new ConflictException("You've reached your recipe-image storage limit");
        }

        try {
            Files.createDirectories(IMAGE_DIR);
            // Clear out any existing file for this recipe first, under any extension -- a
            // same-recipe re-upload that changes format (e.g. jpg -> png) would otherwise leave
            // the old file behind forever, still on disk and still counted against the quota.
            deleteImageFilesIfAny(recipeId);
            Files.write(IMAGE_DIR.resolve(recipeId + "." + extension), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save recipe image", e);
        }
        // Cache-bust: the file is always saved under the same name, so without a changing URL
        // Flutter's (and any HTTP) image cache would keep showing the old photo after a re-upload.
        recipe.setImageUrl("/recipe-images/" + recipeId + "." + extension + "?t=" + System.currentTimeMillis());
        recipe = recipeRepository.save(recipe);
        return toResponse(recipe, recipeIngredientRepository.findByRecipeId(recipeId), recipeTagRepository.findByRecipeId(recipeId));
    }

    private String sniffImageExtension(byte[] bytes) {
        if (startsWith(bytes, PNG_MAGIC)) {
            return "png";
        }
        if (startsWith(bytes, JPEG_MAGIC)) {
            return "jpg";
        }
        // WEBP: "RIFF" (bytes 0-3), then a 4-byte chunk size, then "WEBP" (bytes 8-11).
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private void deleteImageFilesIfAny(UUID recipeId) {
        for (String ext : IMAGE_EXTENSIONS) {
            try {
                Files.deleteIfExists(IMAGE_DIR.resolve(recipeId + "." + ext));
            } catch (IOException e) {
                // Best-effort cleanup; an orphaned file is a disk-space nuisance, not a correctness
                // problem (nothing will ever reference recipeId again once its row is gone/replaced).
            }
        }
    }

    private long existingImageBytes(UUID recipeId) {
        for (String ext : IMAGE_EXTENSIONS) {
            Path path = IMAGE_DIR.resolve(recipeId + "." + ext);
            if (Files.exists(path)) {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /** Sums the on-disk size of every image file across every recipe this user owns. Recipes are
     * globally visible, not household-scoped (see {@link Recipe}'s own doc comment), so this is
     * necessarily a per-*user* quota rather than per-household. */
    private long currentImageUsageBytes(UUID userId) {
        return recipeRepository.findByCreatedByUserId(userId).stream()
                .mapToLong(r -> existingImageBytes(r.getId()))
                .sum();
    }

    private Recipe requireOwnedRecipe(UUID recipeId, UUID requesterUserId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        if (recipe.getCreatedByUserId() == null || !recipe.getCreatedByUserId().equals(requesterUserId)) {
            throw new ForbiddenException("Only the recipe's creator can modify it");
        }
        return recipe;
    }

    public List<RecipeResponse> listRecipes(UUID requesterUserId, UUID householdId, List<String> tagValues, StockFilter stockFilter) {
        List<Recipe> visible = recipeRepository.findAllVisibleTo(requesterUserId);
        if (visible.isEmpty()) {
            return List.of();
        }

        List<UUID> recipeIds = visible.stream().map(Recipe::getId).toList();
        Map<UUID, List<RecipeIngredient>> ingredientsByRecipe = recipeIngredientRepository.findByRecipeIdIn(recipeIds)
                .stream().collect(Collectors.groupingBy(RecipeIngredient::getRecipeId));
        Map<UUID, List<RecipeTag>> tagsByRecipe = recipeTagRepository.findByRecipeIdIn(recipeIds)
                .stream().collect(Collectors.groupingBy(RecipeTag::getRecipeId));

        Map<UUID, Ingredient> ingredientsById = RepositoryUtils.findAllByIdAsMap(ingredientRepository,
                ingredientsByRecipe.values().stream().flatMap(List::stream)
                        .map(RecipeIngredient::getIngredientId).distinct().toList(), Ingredient::getId);

        Set<String> wantedTagValues = tagValues == null ? Set.of() :
                tagValues.stream().map(String::toLowerCase).filter(StringUtils::hasText).collect(Collectors.toSet());
        Exclusions exclusions = householdId != null ? computeHouseholdExclusions(householdId) : Exclusions.EMPTY;

        // Binary per-ingredient presence in the household's pantry -- no unit conversion (see project notes),
        // just "is this ingredient stocked at all", used for the stock-aware sort/filter below.
        Set<UUID> pantryIngredientIds = householdId != null
                ? pantryItemRepository.findByHouseholdId(householdId).stream().map(PantryItem::getIngredientId).collect(Collectors.toSet())
                : Set.of();

        List<Recipe> filtered = visible.stream()
                .filter(r -> wantedTagValues.isEmpty() || tagsByRecipe.getOrDefault(r.getId(), List.of()).stream()
                        .map(t -> t.getValue().toLowerCase()).collect(Collectors.toSet()).containsAll(wantedTagValues))
                .filter(r -> !isExcluded(r, exclusions, ingredientsByRecipe, tagsByRecipe, ingredientsById))
                .filter(r -> stockFilter != StockFilter.ONLY_STOCK || householdId == null
                        || stockCoverage(r, ingredientsByRecipe, pantryIngredientIds) >= 1.0)
                .toList();

        if (stockFilter == StockFilter.PRIORITIZE && householdId != null) {
            filtered = filtered.stream()
                    .sorted(Comparator.comparingDouble(
                            (Recipe r) -> stockCoverage(r, ingredientsByRecipe, pantryIngredientIds)).reversed())
                    .toList();
        }

        return filtered.stream()
                .map(r -> {
                    RecipeResponse response = toResponse(r,
                            ingredientsByRecipe.getOrDefault(r.getId(), List.of()),
                            tagsByRecipe.getOrDefault(r.getId(), List.of()),
                            ingredientsById);
                    return householdId != null
                            ? response.withStockCoverage(stockCoverage(r, ingredientsByRecipe, pantryIngredientIds))
                            : response;
                })
                .toList();
    }

    private double stockCoverage(Recipe recipe, Map<UUID, List<RecipeIngredient>> ingredientsByRecipe, Set<UUID> pantryIngredientIds) {
        List<RecipeIngredient> ris = ingredientsByRecipe.getOrDefault(recipe.getId(), List.of());
        if (ris.isEmpty()) {
            return 1.0;
        }
        long matched = ris.stream().filter(ri -> pantryIngredientIds.contains(ri.getIngredientId())).count();
        return (double) matched / ris.size();
    }

    private boolean isExcluded(Recipe recipe, Exclusions exclusions,
                                Map<UUID, List<RecipeIngredient>> ingredientsByRecipe,
                                Map<UUID, List<RecipeTag>> tagsByRecipe,
                                Map<UUID, Ingredient> ingredientsById) {
        if (exclusions.isEmpty()) {
            return false;
        }
        boolean allergyHit = ingredientsByRecipe.getOrDefault(recipe.getId(), List.of()).stream()
                .map(ri -> ingredientsById.get(ri.getIngredientId()))
                .filter(Objects::nonNull)
                .anyMatch(ing -> exclusions.allergyNames().stream()
                        .anyMatch(a -> ingredientMatchesAllergen(ing.getName().toLowerCase(), a)));
        if (allergyHit) {
            return true;
        }
        return tagsByRecipe.getOrDefault(recipe.getId(), List.of()).stream()
                .anyMatch(tag -> exclusions.forbiddenTasteKeys().contains(tasteKey(tag.getTagType(), tag.getValue())));
    }

    /**
     * Exclusions now come from the household's own allergy/taste lists -- one-time-inherited from
     * the union of member preferences (see {@link PreferenceService#ensureHouseholdPreferencesInherited})
     * and independently editable per household from there, rather than a live re-union of each
     * member's current personal preferences on every call. Calling ensureHouseholdPreferencesInherited
     * here (not just from the household-preferences screen) means filtering reflects the inherited
     * set even for a household whose members never open that screen.
     */
    private Exclusions computeHouseholdExclusions(UUID householdId) {
        preferenceService.ensureHouseholdPreferencesInherited(householdId);

        Set<String> allergyNames = allergyRepository.findAllById(
                        householdAllergyRepository.findByHouseholdId(householdId).stream()
                                .map(HouseholdAllergy::getAllergyId).distinct().toList())
                .stream().map(a -> a.getName().toLowerCase()).collect(Collectors.toSet());

        Set<UUID> forbiddenTasteIds = householdTasteRepository.findByHouseholdId(householdId).stream()
                .filter(ht -> ht.getPreference() == TastePreference.FORBIDDEN)
                .map(HouseholdTaste::getTasteId)
                .collect(Collectors.toSet());

        Set<String> forbiddenTasteKeys = tasteRepository.findAllById(forbiddenTasteIds).stream()
                .map(t -> tasteKey(t.getType(), t.getName()))
                .collect(Collectors.toSet());

        return new Exclusions(allergyNames, forbiddenTasteKeys);
    }

    private String tasteKey(TasteType type, String value) {
        return type + "|" + value.toLowerCase();
    }

    /**
     * Substring match with a cheap singular/plural allowance: allergy reference names are often
     * plural ("Peanuts", "Eggs") while ingredient names are commonly singular ("Peanut Butter").
     */
    private boolean ingredientMatchesAllergen(String ingredientNameLower, String allergyNameLower) {
        if (ingredientNameLower.contains(allergyNameLower)) {
            return true;
        }
        if (allergyNameLower.endsWith("s")) {
            String singular = allergyNameLower.substring(0, allergyNameLower.length() - 1);
            if (ingredientNameLower.contains(singular)) {
                return true;
            }
        }
        return false;
    }

    private List<RecipeIngredient> saveIngredients(UUID recipeId, List<RecipeIngredientRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(req -> {
                    Ingredient ingredient = ingredientService.resolveGlobalOrCreate(req.ingredientId(), req.ingredientName());
                    return recipeIngredientRepository.save(RecipeIngredient.builder()
                            .recipeId(recipeId)
                            .ingredientId(ingredient.getId())
                            .quantity(req.quantity())
                            .unit(req.unit())
                            .build());
                })
                .toList();
    }

    private List<RecipeTag> saveTags(UUID recipeId, List<RecipeTagRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(req -> recipeTagRepository.save(RecipeTag.builder()
                        .recipeId(recipeId)
                        .tagType(req.type())
                        .value(req.value())
                        .build()))
                .toList();
    }

    private RecipeResponse toResponse(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeTag> tags) {
        Map<UUID, Ingredient> ingredientsById = RepositoryUtils.findAllByIdAsMap(ingredientRepository,
                ingredients.stream().map(RecipeIngredient::getIngredientId).distinct().toList(), Ingredient::getId);
        return toResponse(recipe, ingredients, tags, ingredientsById);
    }

    private RecipeResponse toResponse(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeTag> tags,
                                       Map<UUID, Ingredient> ingredientsById) {
        List<RecipeIngredientResponse> ingredientResponses = ingredients.stream()
                .map(ri -> new RecipeIngredientResponse(
                        ri.getIngredientId(),
                        Optional.ofNullable(ingredientsById.get(ri.getIngredientId())).map(Ingredient::getName).orElse(null),
                        ri.getQuantity(), ri.getUnit()))
                .toList();
        List<RecipeTagResponse> tagResponses = tags.stream()
                .map(t -> new RecipeTagResponse(t.getTagType(), t.getValue()))
                .toList();
        RecipeResponse.NutritionEstimate estimate = recipe.getCaloriesPerServing() == null
                ? estimateNutrition(ingredients, ingredientsById, recipe.getServings())
                : null;
        return RecipeResponse.from(recipe, ingredientResponses, tagResponses, estimate);
    }

    /**
     * Best-effort per-serving nutrition computed from the ingredient list, used as a fallback when
     * the recipe author didn't enter caloriesPerServing directly. See NutritionEstimator for the
     * unit-conversion/summation logic (shared with ExternalRecipeService's live-search variant).
     */
    private RecipeResponse.NutritionEstimate estimateNutrition(List<RecipeIngredient> ingredients,
                                                                 Map<UUID, Ingredient> ingredientsById, Integer servings) {
        List<NutritionEstimator.IngredientNutrition> items = ingredients.stream()
                .map(ri -> {
                    Ingredient ingredient = ingredientsById.get(ri.getIngredientId());
                    return new NutritionEstimator.IngredientNutrition(ri.getQuantity(), ri.getUnit(),
                            ingredient != null ? ingredient.getCaloriesPer100g() : null,
                            ingredient != null ? ingredient.getProteinPer100g() : null,
                            ingredient != null ? ingredient.getCarbsPer100g() : null,
                            ingredient != null ? ingredient.getFatPer100g() : null);
                })
                .toList();
        return NutritionEstimator.estimate(items, servings);
    }

    private record Exclusions(Set<String> allergyNames, Set<String> forbiddenTasteKeys) {
        static final Exclusions EMPTY = new Exclusions(Set.of(), Set.of());

        boolean isEmpty() {
            return allergyNames.isEmpty() && forbiddenTasteKeys.isEmpty();
        }
    }
}
