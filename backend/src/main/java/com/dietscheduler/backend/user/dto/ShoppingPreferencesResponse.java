package com.dietscheduler.backend.user.dto;

import com.dietscheduler.backend.user.User;

public record ShoppingPreferencesResponse(int missingIngredientsLookaheadDays) {
    public static final int DEFAULT_LOOKAHEAD_DAYS = 7;

    public static ShoppingPreferencesResponse from(User u) {
        Integer days = u.getMissingIngredientsLookaheadDays();
        return new ShoppingPreferencesResponse(days != null ? days : DEFAULT_LOOKAHEAD_DAYS);
    }
}
