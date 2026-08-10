package com.dietscheduler.backend.preferences;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserTasteRepository extends JpaRepository<UserTaste, UUID> {
    List<UserTaste> findByUserId(UUID userId);
    List<UserTaste> findByUserIdIn(List<UUID> userIds);
    Optional<UserTaste> findByUserIdAndTasteId(UUID userId, UUID tasteId);
}
