package com.dietscheduler.backend.pantry.dto;

import com.dietscheduler.backend.pantry.PantryLocation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * All fields optional; only non-null fields are applied (PATCH semantics) -- so every constraint
 * here is deliberately one that Bean Validation treats as satisfied by null (e.g. @PositiveOrZero,
 * @Size), never @NotNull, which would wrongly force every PATCH to resupply every field.
 * ingredientName presence signals the client is also submitting current ingredient edits
 * (name/image/nutrition) -- see PantryService.update, which resolves/forks/updates the ingredient
 * accordingly.
 */
public record UpdatePantryItemRequest(
        PantryLocation location,
        @PositiveOrZero(message = "cannot be negative") BigDecimal quantity,
        @Size(max = 32) String unit,
        LocalDate expirationDate,
        @Size(max = 200) String ingredientName,
        @Size(max = 2048) String ingredientImageUrl,
        @PositiveOrZero @DecimalMax("2000") Double ingredientCaloriesPer100g,
        @PositiveOrZero @DecimalMax("100") Double ingredientProteinPer100g,
        @PositiveOrZero @DecimalMax("100") Double ingredientCarbsPer100g,
        @PositiveOrZero @DecimalMax("100") Double ingredientFatPer100g
) {
}
