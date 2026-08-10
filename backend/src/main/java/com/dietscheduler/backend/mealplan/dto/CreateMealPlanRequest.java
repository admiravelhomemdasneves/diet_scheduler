package com.dietscheduler.backend.mealplan.dto;

import com.dietscheduler.backend.mealplan.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateMealPlanRequest(
        @NotNull LocalDate date,
        @NotNull MealType mealType,
        @NotNull UUID recipeId,
        @Valid List<PortionRequest> portions
) {
}
