package com.dietscheduler.backend.pantry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PantryItemRepository extends JpaRepository<PantryItem, UUID> {
    List<PantryItem> findByHouseholdId(UUID householdId);
    boolean existsByIngredientId(UUID ingredientId);
    List<PantryItem> findByHouseholdIdAndIngredientIdAndUnit(UUID householdId, UUID ingredientId, String unit);
    void deleteByHouseholdId(UUID householdId);
}
