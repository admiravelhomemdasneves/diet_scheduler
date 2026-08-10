package com.dietscheduler.backend.user.dto;

import com.dietscheduler.backend.user.UnitSystem;
import jakarta.validation.constraints.NotNull;

public record UpdateUnitSystemRequest(@NotNull UnitSystem unitSystem) {
}
