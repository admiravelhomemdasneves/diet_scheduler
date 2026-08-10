package com.dietscheduler.backend.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateShoppingPreferencesRequest(
        @NotNull @Min(1) Integer missingIngredientsLookaheadDays
) {
}
