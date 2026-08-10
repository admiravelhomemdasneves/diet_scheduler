package com.dietscheduler.backend.mealplan.dto;

/**
 * The requesting user's own combined nutrition for a single day's meal plan (see
 * MealPlanService.getDayNutritionSummary), compared against their nutrition-profile targets.
 * A target/delta field is null only when the user hasn't set that particular target.
 */
public record DayNutritionSummaryResponse(
        double totalCalories,
        double totalProtein,
        double totalCarbs,
        double totalFat,
        Double calorieTarget,
        Double proteinTarget,
        Double carbsTarget,
        Double fatTarget,
        Double calorieDelta,
        Double proteinDelta,
        Double carbsDelta,
        Double fatDelta,
        // True if one or more of the day's meals couldn't contribute (recipe has neither
        // author-entered nor estimable nutrition), meaning the totals may be an underestimate.
        boolean nutritionIncomplete
) {
}
