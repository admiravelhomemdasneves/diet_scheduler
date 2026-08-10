package com.dietscheduler.backend.shoppinglist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record AddMissingIngredientItem(
        @NotNull UUID ingredientId,
        @NotNull @Positive BigDecimal quantity,
        @NotBlank String unit
) {
}
