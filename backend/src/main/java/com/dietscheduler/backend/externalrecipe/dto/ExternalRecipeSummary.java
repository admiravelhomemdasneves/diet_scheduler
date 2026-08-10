package com.dietscheduler.backend.externalrecipe.dto;

import com.dietscheduler.backend.externalrecipe.TheMealDbClient;

public record ExternalRecipeSummary(
        String externalId,
        String name,
        String thumbnailUrl,
        String category,
        String area
) {
    public static ExternalRecipeSummary from(TheMealDbClient.Meal meal) {
        return new ExternalRecipeSummary(meal.id, meal.name, meal.thumbnailUrl, meal.category, meal.area);
    }
}
