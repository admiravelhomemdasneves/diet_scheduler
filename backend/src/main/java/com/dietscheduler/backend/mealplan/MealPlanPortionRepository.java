package com.dietscheduler.backend.mealplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MealPlanPortionRepository extends JpaRepository<MealPlanPortion, UUID> {
    List<MealPlanPortion> findByMealPlanId(UUID mealPlanId);
    List<MealPlanPortion> findByMealPlanIdIn(List<UUID> mealPlanIds);
    void deleteByMealPlanId(UUID mealPlanId);
}
