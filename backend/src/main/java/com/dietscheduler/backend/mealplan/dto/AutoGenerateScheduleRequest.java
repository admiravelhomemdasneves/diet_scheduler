package com.dietscheduler.backend.mealplan.dto;

import com.dietscheduler.backend.mealplan.MealType;
import com.dietscheduler.backend.recipe.StockFilter;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AutoGenerateScheduleRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        List<MealType> mealTypes,
        StockFilter stockFilter,
        Integer avoidRepeatDays,
        Boolean targetNutrition,
        // When true, any existing meal plan within [from,to] matching mealTypes is deleted first,
        // so those slots get a fresh pick instead of being skipped as already-occupied.
        Boolean regenerate
) {
}
