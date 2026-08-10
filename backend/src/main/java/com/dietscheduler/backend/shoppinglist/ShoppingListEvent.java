package com.dietscheduler.backend.shoppinglist;

import com.dietscheduler.backend.shoppinglist.dto.ShoppingListItemResponse;

import java.util.List;

public record ShoppingListEvent(Type type, ShoppingListItemResponse item, List<ShoppingListItemResponse> items) {
    public enum Type {
        SHOPPING_LIST_ITEM_CREATED,
        SHOPPING_LIST_ITEM_UPDATED,
        SHOPPING_LIST_ITEM_DELETED,
        SHOPPING_LIST_REGENERATED
    }

    public static ShoppingListEvent single(Type type, ShoppingListItemResponse item) {
        return new ShoppingListEvent(type, item, null);
    }

    public static ShoppingListEvent regenerated(List<ShoppingListItemResponse> items) {
        return new ShoppingListEvent(Type.SHOPPING_LIST_REGENERATED, null, items);
    }
}
