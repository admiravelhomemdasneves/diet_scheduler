package com.dietscheduler.backend.pantry.dto;

import java.util.UUID;

/**
 * id is non-null only for a household's own custom ingredients (picking one reuses that exact
 * row); Open Food Facts results have no id yet -- one is created only if actually added.
 */
public record IngredientSuggestion(
        UUID id,
        String name,
        String barcode,
        String imageUrl,
        String category,
        Double caloriesPer100g,
        Double proteinPer100g,
        Double carbsPer100g,
        Double fatPer100g,
        boolean custom
) {
}
