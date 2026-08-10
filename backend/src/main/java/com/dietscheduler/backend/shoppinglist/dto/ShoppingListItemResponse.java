package com.dietscheduler.backend.shoppinglist.dto;

import com.dietscheduler.backend.pantry.Ingredient;
import com.dietscheduler.backend.shoppinglist.ShoppingListItem;
import com.dietscheduler.backend.shoppinglist.ShoppingListSource;

import java.math.BigDecimal;
import java.util.UUID;

public record ShoppingListItemResponse(
        UUID id,
        UUID householdId,
        UUID ingredientId,
        String ingredientName,
        BigDecimal quantity,
        String unit,
        boolean checked,
        ShoppingListSource source
) {
    public static ShoppingListItemResponse from(ShoppingListItem item, Ingredient ingredient) {
        return new ShoppingListItemResponse(
                item.getId(), item.getHouseholdId(), item.getIngredientId(),
                ingredient != null ? ingredient.getName() : null,
                item.getQuantity(), item.getUnit(), item.isChecked(), item.getSource());
    }
}
