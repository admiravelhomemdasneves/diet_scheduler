package com.dietscheduler.backend.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "notification_setting", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSetting {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lunch_time", nullable = false)
    @Builder.Default
    private LocalTime lunchTime = LocalTime.of(12, 30);

    @Column(name = "dinner_time", nullable = false)
    @Builder.Default
    private LocalTime dinnerTime = LocalTime.of(19, 30);

    @Column(name = "snack_time", nullable = false)
    @Builder.Default
    private LocalTime snackTime = LocalTime.of(16, 0);

    @Column(name = "daily_meal_reminder_enabled", nullable = false)
    @Builder.Default
    private boolean dailyMealReminderEnabled = true;

    @Column(name = "start_cooking_reminder_enabled", nullable = false)
    @Builder.Default
    private boolean startCookingReminderEnabled = true;

    @Column(name = "low_stock_alert_enabled", nullable = false)
    @Builder.Default
    private boolean lowStockAlertEnabled = true;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private double lowStockThreshold = 1.0;
}
