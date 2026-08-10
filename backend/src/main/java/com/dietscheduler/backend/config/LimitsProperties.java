package com.dietscheduler.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code dietscheduler.limits.*}. Bounds how large a date range a client can request in one
 * call to auto-generate/list a meal plan or look ahead for missing shopping-list ingredients --
 * without a cap, a client-supplied range spanning years turns into a day-by-day loop of DB writes
 * (auto-generate) or reads (list/lookahead) in a single request/transaction.
 */
@Validated
@ConfigurationProperties(prefix = "dietscheduler.limits")
public record LimitsProperties(
        @Positive int maxPlanRangeDays,
        @Positive int maxLookaheadDays
) {
}
