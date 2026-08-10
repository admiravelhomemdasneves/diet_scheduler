package com.dietscheduler.backend.externalrecipe.dto;

import java.math.BigDecimal;

public record ExternalRecipeIngredient(String name, BigDecimal quantity, String unit) {
}
