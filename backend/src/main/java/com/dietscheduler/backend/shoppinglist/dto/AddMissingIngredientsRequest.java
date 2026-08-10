package com.dietscheduler.backend.shoppinglist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// @Valid on the list element type is required, not decorative -- without it Bean Validation
// only checks that the list itself is non-empty; AddMissingIngredientItem's own @NotNull/
// @Positive/@NotBlank constraints on each item are silently skipped.
public record AddMissingIngredientsRequest(@NotEmpty List<@Valid AddMissingIngredientItem> items) {
}
