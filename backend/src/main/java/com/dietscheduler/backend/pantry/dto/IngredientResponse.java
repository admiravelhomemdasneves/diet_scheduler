package com.dietscheduler.backend.pantry.dto;

import com.dietscheduler.backend.pantry.Ingredient;

import java.util.UUID;

public record IngredientResponse(
        UUID id,
        String name,
        String category,
        String defaultUnit,
        Double caloriesPer100g,
        Double proteinPer100g,
        Double carbsPer100g,
        Double fatPer100g,
        String barcode,
        String imageUrl,
        boolean custom
) {
    public static IngredientResponse from(Ingredient i) {
        return new IngredientResponse(
                i.getId(), i.getName(), i.getCategory(), i.getDefaultUnit(),
                i.getCaloriesPer100g(), i.getProteinPer100g(), i.getCarbsPer100g(), i.getFatPer100g(),
                i.getBarcode(), i.getImageUrl(), i.getHouseholdId() != null);
    }
}
