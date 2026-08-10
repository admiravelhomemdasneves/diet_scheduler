package com.dietscheduler.backend.shoppinglist.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AddMissingIngredientItem(
        @NotNull UUID ingredientId,
        @NotNull @Positive @DecimalMax("1000000") BigDecimal quantity,
        @NotBlank @Size(max = 32) String unit
) {
}
