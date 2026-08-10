package com.dietscheduler.backend.shoppinglist.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record IgnoreMissingIngredientsRequest(@NotEmpty List<MissingIngredientKey> items) {
}
