package com.dietscheduler.backend.household;

import com.dietscheduler.backend.household.dto.CreateHouseholdRequest;
import com.dietscheduler.backend.household.dto.HouseholdResponse;
import com.dietscheduler.backend.household.dto.InviteCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody CreateHouseholdRequest request) {
        Household household = householdService.createHousehold(userId, request.name());
        return householdService.toResponse(household);
    }

    @GetMapping("/{id}")
    public HouseholdResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        Household household = householdService.getHouseholdForMember(id, userId);
        return householdService.toResponse(household);
    }

    @PostMapping("/{id}/invite")
    public InviteCodeResponse invite(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return new InviteCodeResponse(householdService.getInviteCodeForOwner(id, userId));
    }

    @PostMapping("/join/{inviteCode}")
    public HouseholdResponse join(@AuthenticationPrincipal UUID userId, @PathVariable String inviteCode) {
        HouseholdMember membership = householdService.joinByInviteCode(inviteCode, userId);
        Household household = householdService.getHouseholdForMember(membership.getHouseholdId(), userId);
        return householdService.toResponse(household);
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        householdService.leaveHousehold(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        householdService.deleteHousehold(id, userId);
    }
}
