package com.dietscheduler.backend.household.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateHouseholdRequest(@NotBlank String name) {
}
