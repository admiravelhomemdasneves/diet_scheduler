package com.dietscheduler.backend.pantry.dto;

import jakarta.validation.constraints.NotBlank;

/** Used for both creating and updating a household's custom ingredient -- the small edit form always submits full state. */
public record CustomIngredientRequest(
        @NotBlank String name,
        String imageUrl,
        String category,
        Double caloriesPer100g,
        Double proteinPer100g,
        Double carbsPer100g,
        Double fatPer100g
) {
}
