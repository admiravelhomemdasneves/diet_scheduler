package com.dietscheduler.backend.pantry.dto;

import com.dietscheduler.backend.pantry.PantryLocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePantryItemRequest(
        UUID ingredientId,
        String ingredientName,
        // Optional enrichment for a newly-created (by name) ingredient -- typically a search
        // suggestion the user picked client-side, so the server doesn't need to re-look it up.
        String ingredientBarcode,
        String ingredientImageUrl,
        String ingredientCategory,
        Double ingredientCaloriesPer100g,
        Double ingredientProteinPer100g,
        Double ingredientCarbsPer100g,
        Double ingredientFatPer100g,
        @NotNull PantryLocation location,
        @NotNull @Positive BigDecimal quantity,
        @NotBlank String unit,
        LocalDate expirationDate
) {
}
