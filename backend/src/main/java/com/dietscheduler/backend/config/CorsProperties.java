package com.dietscheduler.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Binds {@code dietscheduler.cors.allowed-origins} -- a comma-separated allowlist of browser
 * origins permitted to call the API cross-origin. Empty by default, which is the correct secure
 * default here: the mobile app doesn't send an Origin header at all (CORS is a browser-enforced
 * mechanism), so this only ever needs to be non-empty for a future browser-based client.
 */
@ConfigurationProperties(prefix = "dietscheduler.cors")
public record CorsProperties(String allowedOrigins) {

    public List<String> allowedOriginList() {
        if (!StringUtils.hasText(allowedOrigins)) {
            return List.of();
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
