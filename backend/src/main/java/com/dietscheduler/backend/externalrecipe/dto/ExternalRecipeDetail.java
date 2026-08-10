package com.dietscheduler.backend.externalrecipe.dto;

import com.dietscheduler.backend.recipe.RecipeCategory;
import com.dietscheduler.backend.recipe.dto.RecipeTagRequest;

import java.util.List;

/** A read-only preview of an external recipe, fetched live -- nothing is persisted until favorited. */
public record ExternalRecipeDetail(
        String externalId,
        String name,
        RecipeCategory category,
        String instructions,
        Integer servings,
        String imageUrl,
        List<ExternalRecipeIngredient> ingredients,
        List<RecipeTagRequest> tags,
        // Best-effort estimate from a live Open Food Facts search per ingredient -- TheMealDB
        // itself has no nutrition data, so unlike a saved Recipe there's no author-entered value
        // to prefer; this is always the estimate (or null if nothing could be computed at all).
        Double estimatedCaloriesPerServing,
        Double estimatedProteinPerServing,
        Double estimatedCarbsPerServing,
        Double estimatedFatPerServing,
        boolean nutritionEstimateIncomplete
) {
}
