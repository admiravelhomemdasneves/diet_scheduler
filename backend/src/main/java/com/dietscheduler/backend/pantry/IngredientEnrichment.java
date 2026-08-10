package com.dietscheduler.backend.pantry;

/**
 * Optional nutrition/image data passed along by the client when creating a NEW ingredient by name --
 * typically the exact suggestion the user picked from the search dropdown, so we don't have to
 * re-search Open Food Facts server-side for data we already showed them client-side.
 */
public record IngredientEnrichment(
        String barcode,
        String imageUrl,
        String category,
        Double caloriesPer100g,
        Double proteinPer100g,
        Double carbsPer100g,
        Double fatPer100g
) {
    public boolean isEmpty() {
        return barcode == null && imageUrl == null && category == null
                && caloriesPer100g == null && proteinPer100g == null && carbsPer100g == null && fatPer100g == null;
    }
}
