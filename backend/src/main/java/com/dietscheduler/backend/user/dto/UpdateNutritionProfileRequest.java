package com.dietscheduler.backend.user.dto;

/** All fields optional; only non-null fields are applied (PATCH semantics). */
public record UpdateNutritionProfileRequest(
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
}
