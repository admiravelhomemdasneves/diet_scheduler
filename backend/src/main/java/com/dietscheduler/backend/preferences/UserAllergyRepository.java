package com.dietscheduler.backend.preferences;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAllergyRepository extends JpaRepository<UserAllergy, UUID> {
    List<UserAllergy> findByUserId(UUID userId);
    List<UserAllergy> findByUserIdIn(List<UUID> userIds);
    Optional<UserAllergy> findByUserIdAndAllergyId(UUID userId, UUID allergyId);
}
