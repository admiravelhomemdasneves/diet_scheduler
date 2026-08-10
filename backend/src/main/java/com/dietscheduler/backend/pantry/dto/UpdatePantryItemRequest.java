package com.dietscheduler.backend.pantry.dto;

import com.dietscheduler.backend.pantry.PantryLocation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * All fields optional; only non-null fields are applied (PATCH semantics). ingredientName
 * presence signals the client is also submitting current ingredient edits (name/image/nutrition)
 * -- see PantryService.update, which resolves/forks/updates the ingredient accordingly.
 */
public record UpdatePantryItemRequest(
        PantryLocation location,
        BigDecimal quantity,
        String unit,
        LocalDate expirationDate,
        String ingredientName,
        String ingredientImageUrl,
        Double ingredientCaloriesPer100g,
        Double ingredientProteinPer100g,
        Double ingredientCarbsPer100g,
        Double ingredientFatPer100g
) {
}
