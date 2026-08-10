package com.dietscheduler.backend.preferences;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseholdTasteRepository extends JpaRepository<HouseholdTaste, UUID> {
    List<HouseholdTaste> findByHouseholdId(UUID householdId);
    Optional<HouseholdTaste> findByHouseholdIdAndTasteId(UUID householdId, UUID tasteId);
    void deleteByHouseholdId(UUID householdId);
}
