package com.dietscheduler.backend.preferences;

import com.dietscheduler.backend.preferences.dto.AllergyResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/households/{householdId}/allergies")
public class HouseholdAllergyController {

    private final PreferenceService preferenceService;

    public HouseholdAllergyController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public List<AllergyResponse> getAllergies(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId) {
        return preferenceService.getHouseholdAllergies(householdId, userId).stream().map(AllergyResponse::from).toList();
    }

    @PutMapping("/{allergyId}")
    public void addAllergy(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId, @PathVariable UUID allergyId) {
        preferenceService.addHouseholdAllergy(householdId, userId, allergyId);
    }

    @DeleteMapping("/{allergyId}")
    public void removeAllergy(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId, @PathVariable UUID allergyId) {
        preferenceService.removeHouseholdAllergy(householdId, userId, allergyId);
    }
}
