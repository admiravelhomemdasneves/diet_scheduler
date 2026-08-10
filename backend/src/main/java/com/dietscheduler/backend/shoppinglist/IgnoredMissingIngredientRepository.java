package com.dietscheduler.backend.shoppinglist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IgnoredMissingIngredientRepository extends JpaRepository<IgnoredMissingIngredient, UUID> {
    List<IgnoredMissingIngredient> findByHouseholdId(UUID householdId);
    boolean existsByHouseholdIdAndIngredientIdAndUnit(UUID householdId, UUID ingredientId, String unit);
    void deleteByHouseholdId(UUID householdId);
}
