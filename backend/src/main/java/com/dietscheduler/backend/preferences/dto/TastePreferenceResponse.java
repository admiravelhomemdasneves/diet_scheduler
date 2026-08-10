package com.dietscheduler.backend.preferences.dto;

import com.dietscheduler.backend.preferences.Taste;
import com.dietscheduler.backend.preferences.TastePreference;

import java.util.UUID;

public record TastePreferenceResponse(UUID tasteId, String tasteName, com.dietscheduler.backend.preferences.TasteType type, TastePreference preference) {
    public static TastePreferenceResponse of(Taste taste, TastePreference preference) {
        return new TastePreferenceResponse(taste.getId(), taste.getName(), taste.getType(), preference);
    }
}
