package com.dietscheduler.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Binds {@code dietscheduler.images.*} -- bounds on recipe photo uploads (see
 * RecipeService.updateImage). {@code maxBytes} is enforced in addition to Spring's own
 * {@code spring.servlet.multipart.max-file-size}, since that limit is shared across every
 * multipart endpoint in the app, not specific to images. */
@Validated
@ConfigurationProperties(prefix = "dietscheduler.images")
public record ImagesProperties(
        @Positive long maxBytes,
        @Positive long perUserQuotaBytes
) {
}
