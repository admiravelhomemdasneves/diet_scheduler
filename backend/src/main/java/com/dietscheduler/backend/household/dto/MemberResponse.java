package com.dietscheduler.backend.household.dto;

import com.dietscheduler.backend.household.HouseholdRole;

import java.util.UUID;

public record MemberResponse(UUID userId, String displayName, String email, HouseholdRole role) {
}
