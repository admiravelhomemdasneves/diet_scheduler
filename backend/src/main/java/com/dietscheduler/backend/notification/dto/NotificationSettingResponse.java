package com.dietscheduler.backend.notification.dto;

import com.dietscheduler.backend.notification.NotificationSetting;

import java.time.LocalTime;

public record NotificationSettingResponse(
        LocalTime lunchTime,
        LocalTime dinnerTime,
        LocalTime snackTime,
        boolean dailyMealReminderEnabled,
        boolean startCookingReminderEnabled,
        boolean lowStockAlertEnabled,
        double lowStockThreshold
) {
    public static NotificationSettingResponse from(NotificationSetting s) {
        return new NotificationSettingResponse(s.getLunchTime(), s.getDinnerTime(), s.getSnackTime(),
                s.isDailyMealReminderEnabled(), s.isStartCookingReminderEnabled(), s.isLowStockAlertEnabled(),
                s.getLowStockThreshold());
    }
}
