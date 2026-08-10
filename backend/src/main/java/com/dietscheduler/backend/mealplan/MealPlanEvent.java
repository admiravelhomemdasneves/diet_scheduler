package com.dietscheduler.backend.mealplan;

import com.dietscheduler.backend.mealplan.dto.MealPlanResponse;

public record MealPlanEvent(Type type, MealPlanResponse mealPlan) {
    public enum Type {
        MEAL_PLAN_CREATED,
        MEAL_PLAN_UPDATED,
        MEAL_PLAN_DELETED
    }
}
