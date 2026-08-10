package com.dietscheduler.backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, UUID> {
    Optional<NotificationSetting> findByUserId(UUID userId);
}
