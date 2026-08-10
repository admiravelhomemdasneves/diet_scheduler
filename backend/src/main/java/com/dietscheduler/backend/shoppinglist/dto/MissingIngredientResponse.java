package com.dietscheduler.backend.shoppinglist.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** One ingredient+unit shortfall between what upcoming meal-plan recipes need and what's in the pantry. */
public record MissingIngredientResponse(
        UUID ingredientId,
        String ingredientName,
        BigDecimal quantity,
        String unit
) {
}
