package com.dietscheduler.backend.pantry;

import com.dietscheduler.backend.pantry.dto.PantryItemResponse;

public record PantryEvent(Type type, PantryItemResponse item) {
    public enum Type {
        PANTRY_ITEM_CREATED,
        PANTRY_ITEM_UPDATED,
        PANTRY_ITEM_DELETED
    }
}
