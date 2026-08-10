package com.dietscheduler.backend.mealplan.dto;

import com.dietscheduler.backend.mealplan.MealType;
import com.dietscheduler.backend.recipe.StockFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AutoGenerateScheduleRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        List<MealType> mealTypes,
        StockFilter stockFilter,
        // Feeds a LocalDate.minusDays() call server-side; bounded so an extreme value can't push
        // that computation outside LocalDate's representable range.
        @Min(0) @Max(365) Integer avoidRepeatDays,
        Boolean targetNutrition,
        // When true, any existing meal plan within [from,to] matching mealTypes is deleted first,
        // so those slots get a fresh pick instead of being skipped as already-occupied.
        Boolean regenerate
) {
}
