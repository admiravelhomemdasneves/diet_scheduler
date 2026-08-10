package com.dietscheduler.backend.recipe.dto;

import com.dietscheduler.backend.preferences.TasteType;

public record RecipeTagResponse(TasteType type, String value) {
}
