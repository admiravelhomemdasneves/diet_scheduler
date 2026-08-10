package com.dietscheduler.backend.recipe.dto;

import com.dietscheduler.backend.preferences.TasteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecipeTagRequest(@NotNull TasteType type, @NotBlank String value) {
}
