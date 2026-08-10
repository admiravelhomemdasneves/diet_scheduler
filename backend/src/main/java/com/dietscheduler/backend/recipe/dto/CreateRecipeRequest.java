package com.dietscheduler.backend.recipe.dto;

import com.dietscheduler.backend.recipe.RecipeCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateRecipeRequest(
        @NotBlank String name,
        @NotNull RecipeCategory category,
        String instructions,
        @NotNull @Positive Integer servings,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        Double caloriesPerServing,
        Double proteinPerServing,
        Double carbsPerServing,
        Double fatPerServing,
        String imageUrl,
        boolean isPrivate,
        @NotEmpty @Valid List<RecipeIngredientRequest> ingredients,
        @Valid List<RecipeTagRequest> tags
) {
}
