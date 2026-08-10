package com.dietscheduler.backend.shoppinglist.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddMissingIngredientsRequest(@NotEmpty List<AddMissingIngredientItem> items) {
}
