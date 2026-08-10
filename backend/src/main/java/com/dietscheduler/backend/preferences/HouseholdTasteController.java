package com.dietscheduler.backend.preferences;

import com.dietscheduler.backend.preferences.dto.SetTastePreferenceRequest;
import com.dietscheduler.backend.preferences.dto.TastePreferenceResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/households/{householdId}/tastes")
public class HouseholdTasteController {

    private final PreferenceService preferenceService;

    public HouseholdTasteController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public List<TastePreferenceResponse> getTastes(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId) {
        return preferenceService.getHouseholdTastes(householdId, userId).stream()
                .map(e -> TastePreferenceResponse.of(e.getKey(), e.getValue()))
                .toList();
    }

    @PutMapping("/{tasteId}")
    public void setTaste(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                          @PathVariable UUID tasteId, @Valid @RequestBody SetTastePreferenceRequest request) {
        preferenceService.setHouseholdTaste(householdId, userId, tasteId, request.preference());
    }

    @DeleteMapping("/{tasteId}")
    public void removeTaste(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId, @PathVariable UUID tasteId) {
        preferenceService.removeHouseholdTaste(householdId, userId, tasteId);
    }
}
