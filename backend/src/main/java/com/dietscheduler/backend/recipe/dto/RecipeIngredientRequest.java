package com.dietscheduler.backend.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeIngredientRequest(
        UUID ingredientId,
        String ingredientName,
        @NotNull @Positive BigDecimal quantity,
        @NotBlank String unit
) {
}
