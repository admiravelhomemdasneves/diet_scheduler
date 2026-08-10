package com.dietscheduler.backend.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** All fields optional; only non-null fields are applied (PATCH semantics). Bounds are generous
 * human-plausible ranges, not clinical limits -- just enough to reject garbage (negative weight,
 * a six-figure calorie target) rather than validate nutrition science. */
public record UpdateNutritionProfileRequest(
        @Size(max = 32) String gender,
        @Min(0) @Max(150) Integer age,
        @PositiveOrZero @DecimalMax("500") Double weight,
        @PositiveOrZero @DecimalMax("300") Double height,
        @PositiveOrZero @DecimalMax("20000") Double calorieTarget,
        @PositiveOrZero @DecimalMax("2000") Double proteinTargetGrams,
        @PositiveOrZero @DecimalMax("2000") Double carbsTargetGrams,
        @PositiveOrZero @DecimalMax("2000") Double fatTargetGrams,
        @PositiveOrZero @DecimalMax("500") Double weightGoalKg
) {
}
