package com.dietscheduler.backend.pantry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {
    Optional<Ingredient> findByNameIgnoreCaseAndHouseholdIdIsNull(String name);
    Optional<Ingredient> findByBarcode(String barcode);
    List<Ingredient> findByHouseholdIdOrderByNameAsc(UUID householdId);
    List<Ingredient> findByHouseholdIdAndNameContainingIgnoreCase(UUID householdId, String query);
    void deleteByHouseholdId(UUID householdId);
}
