package com.dietscheduler.backend.recipe;

import com.dietscheduler.backend.recipe.dto.RecipeResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Best-effort per-serving nutrition computed from a recipe's ingredient list -- shared between
 * saved recipes (RecipeService, using real Ingredient rows) and external/not-yet-favorited
 * recipes (ExternalRecipeService, using a live Open Food Facts name search per ingredient, since
 * those have no Ingredient row yet). Ingredients whose unit can't be converted to a weight
 * (count-type units like "clove"/"unit"/"slice", or an unrecognized unit) or that have no
 * nutrition data of their own are skipped and mark the estimate incomplete, so the UI can show a
 * "this may be an underestimate" warning rather than silently under-count.
 */
public final class NutritionEstimator {

    private NutritionEstimator() {
    }

    private static final Map<String, Double> WEIGHT_UNITS_TO_GRAMS = Map.of(
            "g", 1.0, "kg", 1000.0, "oz", 28.3495, "lb", 453.592);
    // Culinary/volume units approximated at water-like density (1 ml =~ 1 g) -- a simplification,
    // not accurate for e.g. oil or flour, but close enough for a rough per-serving estimate.
    private static final Map<String, Double> VOLUME_UNITS_TO_ML = Map.of(
            "ml", 1.0, "l", 1000.0, "tsp", 4.92892, "tbsp", 14.7868, "cup", 236.588,
            "fl oz", 29.5735, "pint", 473.176, "qt", 946.353, "gal", 3785.41);

    /** One ingredient's contribution to the estimate: its recipe quantity/unit plus its own per-100g nutrition (any of which may be null/unknown). */
    public record IngredientNutrition(BigDecimal quantity, String unit, Double caloriesPer100g,
                                       Double proteinPer100g, Double carbsPer100g, Double fatPer100g) {
    }

    public static RecipeResponse.NutritionEstimate estimate(List<IngredientNutrition> items, Integer servings) {
        if (items.isEmpty() || servings == null || servings <= 0) {
            return null;
        }
        double calories = 0, protein = 0, carbs = 0, fat = 0;
        boolean incomplete = false;
        boolean anyContributed = false;
        for (IngredientNutrition item : items) {
            Double grams = toGrams(item.quantity(), item.unit());
            if (grams == null || item.caloriesPer100g() == null) {
                incomplete = true;
                continue;
            }
            double factor = grams / 100.0;
            calories += item.caloriesPer100g() * factor;
            if (item.proteinPer100g() != null) protein += item.proteinPer100g() * factor; else incomplete = true;
            if (item.carbsPer100g() != null) carbs += item.carbsPer100g() * factor; else incomplete = true;
            if (item.fatPer100g() != null) fat += item.fatPer100g() * factor; else incomplete = true;
            anyContributed = true;
        }
        if (!anyContributed) {
            return null;
        }
        return new RecipeResponse.NutritionEstimate(
                calories / servings, protein / servings, carbs / servings, fat / servings, incomplete);
    }

    static Double toGrams(BigDecimal quantity, String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String u = unit.trim().toLowerCase();
        Double gramsPerUnit = WEIGHT_UNITS_TO_GRAMS.get(u);
        if (gramsPerUnit != null) {
            return quantity.doubleValue() * gramsPerUnit;
        }
        Double mlPerUnit = VOLUME_UNITS_TO_ML.get(u);
        if (mlPerUnit != null) {
            return quantity.doubleValue() * mlPerUnit;
        }
        return null; // count-type ("clove"/"unit"/"slice") or unrecognized unit -- can't convert to a weight
    }
}
