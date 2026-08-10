package com.dietscheduler.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Fails startup immediately -- before any bean is created, before Tomcat binds a port -- if any
 * of the environment variables required for a real deployment are missing. Runs earlier than
 * {@link JwtProperties}'s JSR-380 validation (which additionally rejects a *present but bad*
 * secret), so between the two, every required setting is checked before the app can serve a
 * single request. Reports every missing key at once rather than one failure per restart.
 */
public class RequiredConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final List<String> REQUIRED = List.of(
            "DIETSCHEDULER_JWT_SECRET",
            "DIETSCHEDULER_DB_URL",
            "DIETSCHEDULER_DB_USER",
            "DIETSCHEDULER_DB_PASSWORD"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> missing = REQUIRED.stream()
                .filter(key -> !StringUtils.hasText(environment.getProperty(key)))
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            String list = missing.stream().map(k -> "    - " + k).collect(Collectors.joining("\n"));
            throw new IllegalStateException(
                    "\n\n  DietScheduler cannot start: required configuration is missing.\n"
                            + list + "\n\n"
                            + "  Copy .env.example to .env in the project root and fill it in.\n"
                            + "  Generate a JWT secret with (PowerShell):\n"
                            + "    [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))\n"
                            + "  or (bash):\n"
                            + "    openssl rand -base64 48\n");
        }
    }
}
