package com.dietscheduler.backend.mealplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MealPlanRepository extends JpaRepository<MealPlan, UUID> {
    List<MealPlan> findByHouseholdIdAndDateBetween(UUID householdId, LocalDate from, LocalDate to);
    List<MealPlan> findByHouseholdId(UUID householdId);
    void deleteByHouseholdId(UUID householdId);
}
