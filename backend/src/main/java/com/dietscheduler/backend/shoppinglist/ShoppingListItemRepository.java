package com.dietscheduler.backend.shoppinglist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, UUID> {
    List<ShoppingListItem> findByHouseholdId(UUID householdId);
    List<ShoppingListItem> findByHouseholdIdAndSource(UUID householdId, ShoppingListSource source);
    Optional<ShoppingListItem> findByHouseholdIdAndIngredientIdAndUnitAndSource(
            UUID householdId, UUID ingredientId, String unit, ShoppingListSource source);
    void deleteByHouseholdIdAndSource(UUID householdId, ShoppingListSource source);
    void deleteByHouseholdId(UUID householdId);
}
