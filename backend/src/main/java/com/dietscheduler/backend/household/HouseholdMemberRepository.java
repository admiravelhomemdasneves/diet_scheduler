package com.dietscheduler.backend.household;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {
    List<HouseholdMember> findByHouseholdId(UUID householdId);
    List<HouseholdMember> findByUserId(UUID userId);
    Optional<HouseholdMember> findByHouseholdIdAndUserId(UUID householdId, UUID userId);
    boolean existsByHouseholdIdAndUserId(UUID householdId, UUID userId);
    void deleteByHouseholdId(UUID householdId);
}
