package com.dietscheduler.backend.notification.dto;

import java.time.LocalTime;

/** All fields optional; only non-null fields are applied (PATCH semantics). */
public record UpdateNotificationSettingRequest(
        LocalTime lunchTime,
        LocalTime dinnerTime,
        LocalTime snackTime,
        Boolean dailyMealReminderEnabled,
        Boolean startCookingReminderEnabled,
        Boolean lowStockAlertEnabled,
        Double lowStockThreshold
) {
}
