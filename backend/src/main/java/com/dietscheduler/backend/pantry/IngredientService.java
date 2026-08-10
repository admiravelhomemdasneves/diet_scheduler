package com.dietscheduler.backend.pantry;

import com.dietscheduler.backend.common.ConflictException;
import com.dietscheduler.backend.common.ForbiddenException;
import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.pantry.dto.IngredientSuggestion;
import com.dietscheduler.backend.recipe.RecipeIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IngredientService {

    private static final Pattern TRAILING_UNIT = Pattern.compile("([a-zA-Z]+)\\s*$");

    private final IngredientRepository ingredientRepository;
    private final PantryItemRepository pantryItemRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final OpenFoodFactsClient openFoodFactsClient;

    public IngredientService(IngredientRepository ingredientRepository, PantryItemRepository pantryItemRepository,
                              RecipeIngredientRepository recipeIngredientRepository, OpenFoodFactsClient openFoodFactsClient) {
        this.ingredientRepository = ingredientRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.openFoodFactsClient = openFoodFactsClient;
    }

    @Transactional
    public Ingredient findOrCreateByBarcode(String barcode) {
        return ingredientRepository.findByBarcode(barcode)
                .orElseGet(() -> createFromOpenFoodFacts(barcode));
    }

    public Ingredient getById(UUID id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingredient not found"));
    }

    /** The household's own custom ingredients first (so their edits/renames are easy to find), then Open Food Facts matches. */
    public List<IngredientSuggestion> searchOnline(String query, UUID householdId) {
        List<IngredientSuggestion> results = new ArrayList<>(
                ingredientRepository.findByHouseholdIdAndNameContainingIgnoreCase(householdId, query).stream()
                        .map(this::toCustomSuggestion)
                        .toList());
        results.addAll(openFoodFactsClient.search(query).stream().map(this::toSuggestion).toList());
        return results;
    }

    /**
     * Resolves an ingredient by name, creating it if needed. If the client already supplied
     * enrichment data (a picked search suggestion), that's used directly; otherwise -- e.g. the
     * user typed a fully custom name -- we make a best-effort Open Food Facts search ourselves
     * so custom ingredients still end up with real nutrition data where possible. Only ever
     * reuses/creates GLOBAL (non-household) rows -- picking an existing custom ingredient goes
     * through resolveEditableIngredient instead, once it's surfaced (with an id) via search.
     */
    @Transactional
    public Ingredient findOrCreateByName(String name, IngredientEnrichment enrichment) {
        return ingredientRepository.findByNameIgnoreCaseAndHouseholdIdIsNull(name)
                .orElseGet(() -> {
                    if (enrichment != null && !enrichment.isEmpty()) {
                        return ingredientRepository.save(Ingredient.builder()
                                .name(name)
                                .category(enrichment.category())
                                .barcode(safeBarcode(enrichment.barcode()))
                                .imageUrl(enrichment.imageUrl())
                                .caloriesPer100g(enrichment.caloriesPer100g())
                                .proteinPer100g(enrichment.proteinPer100g())
                                .carbsPer100g(enrichment.carbsPer100g())
                                .fatPer100g(enrichment.fatPer100g())
                                .build());
                    }
                    return createFromNameSearch(name);
                });
    }

    /**
     * Resolves an existing ingredient's editable fields (name/image/nutrition), used both when
     * adding a pantry item from a known ingredient (barcode scan or a picked search suggestion)
     * and when editing an existing pantry item's ingredient. If nothing actually changed, the
     * original ingredient is reused as-is. If it changed and the ingredient is already a custom
     * ingredient owned by this household, it's updated in place. Otherwise -- editing a shared
     * global ingredient, or one owned by a different household -- a new custom ingredient is
     * forked for this household so the shared/other household's data is never mutated.
     */
    @Transactional
    public Ingredient resolveEditableIngredient(UUID householdId, UUID baseIngredientId, String name, String imageUrl,
                                                 Double caloriesPer100g, Double proteinPer100g, Double carbsPer100g, Double fatPer100g) {
        Ingredient base = ingredientRepository.findById(baseIngredientId)
                .orElseThrow(() -> new NotFoundException("Ingredient not found"));

        String normalizedName = name.trim();
        String normalizedImageUrl = StringUtils.hasText(imageUrl) ? imageUrl.trim() : null;
        boolean unchanged = base.getName().equals(normalizedName)
                && Objects.equals(base.getImageUrl(), normalizedImageUrl)
                && Objects.equals(base.getCaloriesPer100g(), caloriesPer100g)
                && Objects.equals(base.getProteinPer100g(), proteinPer100g)
                && Objects.equals(base.getCarbsPer100g(), carbsPer100g)
                && Objects.equals(base.getFatPer100g(), fatPer100g);
        if (unchanged) {
            return base;
        }

        boolean ownedByHousehold = householdId.equals(base.getHouseholdId());
        Ingredient target = ownedByHousehold ? base : Ingredient.builder()
                .householdId(householdId)
                .category(base.getCategory())
                .build();
        target.setName(normalizedName);
        target.setImageUrl(normalizedImageUrl);
        target.setCaloriesPer100g(caloriesPer100g);
        target.setProteinPer100g(proteinPer100g);
        target.setCarbsPer100g(carbsPer100g);
        target.setFatPer100g(fatPer100g);
        return ingredientRepository.save(target);
    }

    public List<Ingredient> listCustom(UUID householdId) {
        return ingredientRepository.findByHouseholdIdOrderByNameAsc(householdId);
    }

    @Transactional
    public Ingredient createCustom(UUID householdId, String name, String imageUrl, String category,
                                    Double caloriesPer100g, Double proteinPer100g, Double carbsPer100g, Double fatPer100g) {
        return ingredientRepository.save(Ingredient.builder()
                .householdId(householdId)
                .name(name.trim())
                .imageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null)
                .category(category)
                .caloriesPer100g(caloriesPer100g)
                .proteinPer100g(proteinPer100g)
                .carbsPer100g(carbsPer100g)
                .fatPer100g(fatPer100g)
                .build());
    }

    @Transactional
    public Ingredient updateCustom(UUID householdId, UUID ingredientId, String name, String imageUrl, String category,
                                    Double caloriesPer100g, Double proteinPer100g, Double carbsPer100g, Double fatPer100g) {
        Ingredient ingredient = requireOwnedCustom(householdId, ingredientId);
        ingredient.setName(name.trim());
        ingredient.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null);
        ingredient.setCategory(category);
        ingredient.setCaloriesPer100g(caloriesPer100g);
        ingredient.setProteinPer100g(proteinPer100g);
        ingredient.setCarbsPer100g(carbsPer100g);
        ingredient.setFatPer100g(fatPer100g);
        return ingredientRepository.save(ingredient);
    }

    @Transactional
    public void deleteCustom(UUID householdId, UUID ingredientId) {
        Ingredient ingredient = requireOwnedCustom(householdId, ingredientId);
        if (pantryItemRepository.existsByIngredientId(ingredientId) || recipeIngredientRepository.existsByIngredientId(ingredientId)) {
            throw new ConflictException("This ingredient is still used by a pantry item or recipe -- remove those first");
        }
        ingredientRepository.delete(ingredient);
    }

    private Ingredient requireOwnedCustom(UUID householdId, UUID ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new NotFoundException("Ingredient not found"));
        if (!householdId.equals(ingredient.getHouseholdId())) {
            throw new ForbiddenException("Not a custom ingredient owned by this household");
        }
        return ingredient;
    }

    private Ingredient createFromNameSearch(String name) {
        List<OpenFoodFactsClient.Product> results = openFoodFactsClient.search(name);
        if (results.isEmpty()) {
            return ingredientRepository.save(Ingredient.builder().name(name).build());
        }
        OpenFoodFactsClient.Product best = results.get(0);
        OpenFoodFactsClient.Nutriments n = best.nutriments();
        return ingredientRepository.save(Ingredient.builder()
                .name(name) // keep the user's own typed name, not Open Food Facts' product name
                .category(firstCategory(best.categories()))
                .barcode(safeBarcode(best.code()))
                .imageUrl(best.imageUrl())
                .caloriesPer100g(n != null ? n.energyKcal100g() : null)
                .proteinPer100g(n != null ? n.proteins100g() : null)
                .carbsPer100g(n != null ? n.carbohydrates100g() : null)
                .fatPer100g(n != null ? n.fat100g() : null)
                .build());
    }

    private Ingredient createFromOpenFoodFacts(String barcode) {
        OpenFoodFactsClient.Product product = openFoodFactsClient.lookup(barcode)
                .orElseThrow(() -> new NotFoundException("No product found for barcode " + barcode));

        String name = StringUtils.hasText(product.productName())
                ? product.productName().trim()
                : "Unknown product (" + barcode + ")";

        // A manually-entered global ingredient may already own this exact name; attach the barcode
        // to it instead of violating the (global-only) unique-name convention with a near-duplicate row.
        return ingredientRepository.findByNameIgnoreCaseAndHouseholdIdIsNull(name)
                .map(existing -> {
                    existing.setBarcode(barcode);
                    if (existing.getImageUrl() == null) existing.setImageUrl(product.imageUrl());
                    return ingredientRepository.save(existing);
                })
                .orElseGet(() -> {
                    OpenFoodFactsClient.Nutriments n = product.nutriments();
                    Ingredient ingredient = Ingredient.builder()
                            .name(name)
                            .category(firstCategory(product.categories()))
                            .defaultUnit(guessUnit(product.quantity()))
                            .barcode(barcode)
                            .imageUrl(product.imageUrl())
                            .caloriesPer100g(n != null ? n.energyKcal100g() : null)
                            .proteinPer100g(n != null ? n.proteins100g() : null)
                            .carbsPer100g(n != null ? n.carbohydrates100g() : null)
                            .fatPer100g(n != null ? n.fat100g() : null)
                            .build();
                    return ingredientRepository.save(ingredient);
                });
    }

    private IngredientSuggestion toSuggestion(OpenFoodFactsClient.Product p) {
        OpenFoodFactsClient.Nutriments n = p.nutriments();
        return new IngredientSuggestion(
                null,
                p.productName().trim(),
                p.code(),
                p.imageUrl(),
                firstCategory(p.categories()),
                n != null ? n.energyKcal100g() : null,
                n != null ? n.proteins100g() : null,
                n != null ? n.carbohydrates100g() : null,
                n != null ? n.fat100g() : null,
                false
        );
    }

    private IngredientSuggestion toCustomSuggestion(Ingredient i) {
        return new IngredientSuggestion(
                i.getId(),
                i.getName(),
                i.getBarcode(),
                i.getImageUrl(),
                i.getCategory(),
                i.getCaloriesPer100g(),
                i.getProteinPer100g(),
                i.getCarbsPer100g(),
                i.getFatPer100g(),
                true
        );
    }

    /** Avoids violating Ingredient.barcode's unique constraint when the same product backs two differently-named rows. */
    private String safeBarcode(String barcode) {
        if (!StringUtils.hasText(barcode)) {
            return null;
        }
        return ingredientRepository.findByBarcode(barcode).isPresent() ? null : barcode;
    }

    private String firstCategory(String categories) {
        if (!StringUtils.hasText(categories)) {
            return null;
        }
        return categories.split(",")[0].trim();
    }

    /** Best-effort unit guess from Open Food Facts' free-text "quantity" field (e.g. "500 g", "1L"). */
    private String guessUnit(String quantity) {
        if (!StringUtils.hasText(quantity)) {
            return "g";
        }
        Matcher matcher = TRAILING_UNIT.matcher(quantity.trim());
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return "g";
    }
}
