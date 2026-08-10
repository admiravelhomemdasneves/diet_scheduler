package com.dietscheduler.backend.mealplan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PortionRequest(@NotNull UUID userId, @Positive BigDecimal portionMultiplier) {
}
