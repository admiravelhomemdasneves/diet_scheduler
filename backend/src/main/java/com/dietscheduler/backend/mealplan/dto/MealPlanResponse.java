package com.dietscheduler.backend.mealplan.dto;

import com.dietscheduler.backend.mealplan.MealPlan;
import com.dietscheduler.backend.mealplan.MealType;
import com.dietscheduler.backend.recipe.Recipe;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MealPlanResponse(
        UUID id,
        UUID householdId,
        LocalDate date,
        MealType mealType,
        UUID recipeId,
        String recipeName,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        List<PortionResponse> portions,
        boolean cooked
) {
    public static MealPlanResponse from(MealPlan mp, Recipe recipe, List<PortionResponse> portions) {
        return new MealPlanResponse(mp.getId(), mp.getHouseholdId(), mp.getDate(), mp.getMealType(),
                mp.getRecipeId(), recipe != null ? recipe.getName() : null,
                recipe != null ? recipe.getPrepTimeMinutes() : null, recipe != null ? recipe.getCookTimeMinutes() : null,
                portions, mp.isCooked());
    }
}
