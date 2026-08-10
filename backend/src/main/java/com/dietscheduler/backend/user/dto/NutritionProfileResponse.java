package com.dietscheduler.backend.user.dto;

import com.dietscheduler.backend.user.User;

public record NutritionProfileResponse(
        String gender,
        Integer age,
        Double weight,
        Double height,
        Double calorieTarget,
        Double proteinTargetGrams,
        Double carbsTargetGrams,
        Double fatTargetGrams,
        Double weightGoalKg
) {
    public static NutritionProfileResponse from(User u) {
        return new NutritionProfileResponse(u.getGender(), u.getAge(), u.getWeight(), u.getHeight(),
                u.getCalorieTarget(), u.getProteinTargetGrams(), u.getCarbsTargetGrams(), u.getFatTargetGrams(),
                u.getWeightGoalKg());
    }
}
