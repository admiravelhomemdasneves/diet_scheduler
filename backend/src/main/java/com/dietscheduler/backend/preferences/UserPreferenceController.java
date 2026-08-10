package com.dietscheduler.backend.preferences;

import com.dietscheduler.backend.preferences.dto.AllergyResponse;
import com.dietscheduler.backend.preferences.dto.SetTastePreferenceRequest;
import com.dietscheduler.backend.preferences.dto.TastePreferenceResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/me")
public class UserPreferenceController {

    private final PreferenceService preferenceService;

    public UserPreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping("/allergies")
    public List<AllergyResponse> getAllergies(@AuthenticationPrincipal UUID userId) {
        return preferenceService.getUserAllergies(userId).stream().map(AllergyResponse::from).toList();
    }

    @PutMapping("/allergies/{allergyId}")
    public void addAllergy(@AuthenticationPrincipal UUID userId, @PathVariable UUID allergyId) {
        preferenceService.addUserAllergy(userId, allergyId);
    }

    @DeleteMapping("/allergies/{allergyId}")
    public void removeAllergy(@AuthenticationPrincipal UUID userId, @PathVariable UUID allergyId) {
        preferenceService.removeUserAllergy(userId, allergyId);
    }

    @GetMapping("/tastes")
    public List<TastePreferenceResponse> getTastes(@AuthenticationPrincipal UUID userId) {
        return preferenceService.getUserTastes(userId).stream()
                .map(e -> TastePreferenceResponse.of(e.getKey(), e.getValue()))
                .toList();
    }

    @PutMapping("/tastes/{tasteId}")
    public void setTaste(@AuthenticationPrincipal UUID userId, @PathVariable UUID tasteId,
                          @Valid @RequestBody SetTastePreferenceRequest request) {
        preferenceService.setUserTaste(userId, tasteId, request.preference());
    }

    @DeleteMapping("/tastes/{tasteId}")
    public void removeTaste(@AuthenticationPrincipal UUID userId, @PathVariable UUID tasteId) {
        preferenceService.removeUserTaste(userId, tasteId);
    }
}
