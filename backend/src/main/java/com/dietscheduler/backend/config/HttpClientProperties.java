package com.dietscheduler.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Binds {@code dietscheduler.http-client.*} -- connect/read timeouts applied to every outbound
 * HTTP call this backend makes to a third party (Open Food Facts, TheMealDB, Google token
 * verification). Neither of the two REST clients had any timeout before this, so a slow/hanging
 * upstream could pin a request thread indefinitely. */
@Validated
@ConfigurationProperties(prefix = "dietscheduler.http-client")
public record HttpClientProperties(
        @Positive int connectTimeoutMs,
        @Positive int readTimeoutMs
) {
}
