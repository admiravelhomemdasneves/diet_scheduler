package com.dietscheduler.backend.notification;

import com.dietscheduler.backend.notification.dto.NotificationSettingResponse;
import com.dietscheduler.backend.notification.dto.UpdateNotificationSettingRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users/me/notification-settings")
public class NotificationSettingController {

    private final NotificationSettingRepository repository;

    public NotificationSettingController(NotificationSettingRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public NotificationSettingResponse get(@AuthenticationPrincipal UUID userId) {
        return NotificationSettingResponse.from(findOrCreateDefaults(userId));
    }

    @PatchMapping
    @Transactional
    public NotificationSettingResponse update(@AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateNotificationSettingRequest request) {
        NotificationSetting setting = findOrCreateDefaults(userId);
        if (request.lunchTime() != null) setting.setLunchTime(request.lunchTime());
        if (request.dinnerTime() != null) setting.setDinnerTime(request.dinnerTime());
        if (request.snackTime() != null) setting.setSnackTime(request.snackTime());
        if (request.dailyMealReminderEnabled() != null) setting.setDailyMealReminderEnabled(request.dailyMealReminderEnabled());
        if (request.startCookingReminderEnabled() != null) setting.setStartCookingReminderEnabled(request.startCookingReminderEnabled());
        if (request.lowStockAlertEnabled() != null) setting.setLowStockAlertEnabled(request.lowStockAlertEnabled());
        if (request.lowStockThreshold() != null) setting.setLowStockThreshold(request.lowStockThreshold());
        return NotificationSettingResponse.from(repository.save(setting));
    }

    private NotificationSetting findOrCreateDefaults(UUID userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> repository.save(NotificationSetting.builder().userId(userId).build()));
    }
}
