package com.dietscheduler.backend.household.dto;

import com.dietscheduler.backend.household.Household;

import java.util.List;
import java.util.UUID;

public record HouseholdResponse(UUID id, String name, String inviteCode, List<MemberResponse> members) {
    public static HouseholdResponse from(Household household, List<MemberResponse> members) {
        return new HouseholdResponse(household.getId(), household.getName(), household.getInviteCode(), members);
    }
}
