package com.dietscheduler.backend.preferences.dto;

import com.dietscheduler.backend.preferences.Taste;
import com.dietscheduler.backend.preferences.TasteType;

import java.util.UUID;

public record TasteResponse(UUID id, TasteType type, String name) {
    public static TasteResponse from(Taste t) {
        return new TasteResponse(t.getId(), t.getType(), t.getName());
    }
}
