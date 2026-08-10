package com.dietscheduler.backend.recipe.dto;

import com.dietscheduler.backend.recipe.Recipe;
import com.dietscheduler.backend.recipe.RecipeCategory;

import java.util.List;
import java.util.UUID;

public record RecipeResponse(
        UUID id,
        String name,
        RecipeCategory category,
        String instructions,
        Integer servings,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        Double caloriesPerServing,
        Double proteinPerServing,
        Double carbsPerServing,
        Double fatPerServing,
        // Fallback nutrition computed from the ingredient list, only populated when the author
        // didn't enter caloriesPerServing/etc above -- see RecipeService.estimateNutrition.
        Double estimatedCaloriesPerServing,
        Double estimatedProteinPerServing,
        Double estimatedCarbsPerServing,
        Double estimatedFatPerServing,
        boolean nutritionEstimateIncomplete,
        UUID createdByUserId,
        String imageUrl,
        boolean isPrivate,
        List<RecipeIngredientResponse> ingredients,
        List<RecipeTagResponse> tags,
        Double stockCoverage
) {
    public static RecipeResponse from(Recipe r, List<RecipeIngredientResponse> ingredients, List<RecipeTagResponse> tags,
                                       NutritionEstimate estimate) {
        return new RecipeResponse(
                r.getId(), r.getName(), r.getCategory(), r.getInstructions(), r.getServings(),
                r.getPrepTimeMinutes(), r.getCookTimeMinutes(), r.getCaloriesPerServing(), r.getProteinPerServing(),
                r.getCarbsPerServing(), r.getFatPerServing(),
                estimate != null ? estimate.calories() : null, estimate != null ? estimate.protein() : null,
                estimate != null ? estimate.carbs() : null, estimate != null ? estimate.fat() : null,
                estimate != null && estimate.incomplete(),
                r.getCreatedByUserId(), r.getImageUrl(), r.isPrivate(), ingredients, tags, null);
    }

    public RecipeResponse withStockCoverage(Double coverage) {
        return new RecipeResponse(id, name, category, instructions, servings, prepTimeMinutes, cookTimeMinutes,
                caloriesPerServing, proteinPerServing, carbsPerServing, fatPerServing,
                estimatedCaloriesPerServing, estimatedProteinPerServing, estimatedCarbsPerServing, estimatedFatPerServing,
                nutritionEstimateIncomplete, createdByUserId, imageUrl, isPrivate, ingredients, tags, coverage);
    }

    /** calories/protein/carbs/fat are per-serving; incomplete means one or more ingredients couldn't fully contribute. */
    public record NutritionEstimate(Double calories, Double protein, Double carbs, Double fat, boolean incomplete) {
    }
}
