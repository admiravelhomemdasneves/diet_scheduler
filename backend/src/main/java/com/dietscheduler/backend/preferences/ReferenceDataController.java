package com.dietscheduler.backend.preferences;

import com.dietscheduler.backend.preferences.dto.AllergyResponse;
import com.dietscheduler.backend.preferences.dto.TasteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReferenceDataController {

    private final PreferenceService preferenceService;

    public ReferenceDataController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping("/allergies")
    public List<AllergyResponse> listAllergies() {
        return preferenceService.listAllergies().stream().map(AllergyResponse::from).toList();
    }

    @GetMapping("/tastes")
    public List<TasteResponse> listTastes() {
        return preferenceService.listTastes().stream().map(TasteResponse::from).toList();
    }
}
