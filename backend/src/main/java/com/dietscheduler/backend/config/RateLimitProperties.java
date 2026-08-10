package com.dietscheduler.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code dietscheduler.rate-limit.*} -- per-user, per-minute request caps on the endpoints
 * that either proxy to a third-party API (external recipe search/detail, barcode scan, both
 * unauthenticated-cost-wise from our side but each triggers outbound calls we don't control the
 * volume of) or are otherwise abusable for resource exhaustion (image upload writes to disk;
 * household join is checked against every existing invite code, i.e. a brute-force target).
 */
@Validated
@ConfigurationProperties(prefix = "dietscheduler.rate-limit")
public record RateLimitProperties(
        @Positive int externalRecipePerMinute,
        @Positive int barcodeScanPerMinute,
        @Positive int imageUploadPerMinute,
        @Positive int householdJoinPerMinute,
        @Positive int autoGeneratePerMinute
) {
}
