package com.dietscheduler.backend.mealplan.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PortionResponse(UUID userId, String displayName, BigDecimal portionMultiplier) {
}
