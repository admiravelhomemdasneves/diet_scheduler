package com.dietscheduler.backend.preferences.dto;

import com.dietscheduler.backend.preferences.Allergy;

import java.util.UUID;

public record AllergyResponse(UUID id, String name, String category) {
    public static AllergyResponse from(Allergy a) {
        return new AllergyResponse(a.getId(), a.getName(), a.getCategory());
    }
}
