package com.dietscheduler.backend.shoppinglist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Identifies one missing-ingredient row for the ignore action -- ingredientId+unit, matching how gaps are keyed server-side. */
public record MissingIngredientKey(
        @NotNull UUID ingredientId,
        @NotBlank String unit
) {
}
