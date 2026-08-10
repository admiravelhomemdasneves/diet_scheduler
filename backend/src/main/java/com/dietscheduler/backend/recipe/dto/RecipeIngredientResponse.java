package com.dietscheduler.backend.recipe.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeIngredientResponse(UUID ingredientId, String ingredientName, BigDecimal quantity, String unit) {
}
