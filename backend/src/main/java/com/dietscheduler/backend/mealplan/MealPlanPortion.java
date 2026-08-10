package com.dietscheduler.backend.mealplan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** One row per participating household member -- lets a shared meal have different ration sizes. */
@Entity
@Table(name = "meal_plan_portion", uniqueConstraints = @UniqueConstraint(columnNames = {"meal_plan_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanPortion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "meal_plan_id", nullable = false)
    private UUID mealPlanId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "portion_multiplier", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal portionMultiplier = BigDecimal.ONE;
}
