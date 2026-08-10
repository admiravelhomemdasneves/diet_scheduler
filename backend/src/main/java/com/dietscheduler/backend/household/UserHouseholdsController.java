package com.dietscheduler.backend.household;

import com.dietscheduler.backend.household.dto.HouseholdResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/me/households")
public class UserHouseholdsController {

    private final HouseholdService householdService;

    public UserHouseholdsController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @GetMapping
    public List<HouseholdResponse> list(@AuthenticationPrincipal UUID userId) {
        return householdService.listForUser(userId).stream()
                .map(householdService::toResponse)
                .toList();
    }
}
