package com.dietscheduler.backend.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;
import java.util.Set;

/**
 * Binds {@code dietscheduler.jwt.*}, sourced entirely from environment variables (there is no
 * {@code secret:} key in any committed application*.yml -- see the comment in application.yml).
 * Deliberately validated with JSR-380 rather than a bare {@code @Value}, so a missing or
 * placeholder secret fails Spring context refresh -- before Tomcat binds a port and starts
 * signing tokens with a bad key -- with a message that names the exact problem, instead of a
 * generic "could not resolve placeholder" or, worse, silently starting on a public dev secret.
 */
@Validated
@ConfigurationProperties(prefix = "dietscheduler.jwt")
public record JwtProperties(

        @NotBlank(message = "DIETSCHEDULER_JWT_SECRET is not set. Copy .env.example to .env, "
                + "fill it in, and restart. See .env.example for how to generate one.")
        @Size(min = 32, message = "DIETSCHEDULER_JWT_SECRET must be at least 32 characters "
                + "(HS256 needs >= 256 bits of key material).")
        String secret,

        @Positive(message = "dietscheduler.jwt.expiration-minutes must be positive.")
        long expirationMinutes
) {

    // Every dev-only value this secret has ever defaulted to in source control. Anyone who
    // still has one of these deployed is running on a secret that's public on GitHub.
    private static final Set<String> KNOWN_PLACEHOLDERS = Set.of(
            "dev-only-secret-key-change-me-please-32bytesmin!!",
            "changeme", "change-me", "please-change-me", "secret", "password");

    @AssertTrue(message = "DIETSCHEDULER_JWT_SECRET is a known placeholder/example value, not a "
            + "real secret. Generate a random one -- see .env.example.")
    public boolean isSecretNotAPlaceholder() {
        return secret == null || !KNOWN_PLACEHOLDERS.contains(secret.trim().toLowerCase(Locale.ROOT));
    }
}
