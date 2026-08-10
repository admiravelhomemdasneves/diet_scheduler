package com.dietscheduler.backend.preferences;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseholdAllergyRepository extends JpaRepository<HouseholdAllergy, UUID> {
    List<HouseholdAllergy> findByHouseholdId(UUID householdId);
    Optional<HouseholdAllergy> findByHouseholdIdAndAllergyId(UUID householdId, UUID allergyId);
    void deleteByHouseholdId(UUID householdId);
}
