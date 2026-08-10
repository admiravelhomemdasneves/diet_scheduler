package com.dietscheduler.backend.preferences.dto;

import com.dietscheduler.backend.preferences.TastePreference;
import jakarta.validation.constraints.NotNull;

public record SetTastePreferenceRequest(@NotNull TastePreference preference) {
}
