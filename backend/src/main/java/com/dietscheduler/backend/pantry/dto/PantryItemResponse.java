package com.dietscheduler.backend.pantry.dto;

import com.dietscheduler.backend.pantry.Ingredient;
import com.dietscheduler.backend.pantry.PantryItem;
import com.dietscheduler.backend.pantry.PantryLocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PantryItemResponse(
        UUID id,
        UUID householdId,
        UUID ingredientId,
        String ingredientName,
        String ingredientImageUrl,
        Double ingredientCaloriesPer100g,
        Double ingredientProteinPer100g,
        Double ingredientCarbsPer100g,
        Double ingredientFatPer100g,
        boolean ingredientCustom,
        PantryLocation location,
        BigDecimal quantity,
        String unit,
        LocalDate expirationDate
) {
    public static PantryItemResponse from(PantryItem item, Ingredient ingredient) {
        return new PantryItemResponse(
                item.getId(),
                item.getHouseholdId(),
                item.getIngredientId(),
                ingredient != null ? ingredient.getName() : null,
                ingredient != null ? ingredient.getImageUrl() : null,
                ingredient != null ? ingredient.getCaloriesPer100g() : null,
                ingredient != null ? ingredient.getProteinPer100g() : null,
                ingredient != null ? ingredient.getCarbsPer100g() : null,
                ingredient != null ? ingredient.getFatPer100g() : null,
                ingredient != null && ingredient.getHouseholdId() != null,
                item.getLocation(),
                item.getQuantity(),
                item.getUnit(),
                item.getExpirationDate());
    }
}
