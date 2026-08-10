package com.dietscheduler.backend.shoppinglist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// See the comment on AddMissingIngredientsRequest -- @Valid on the element type is required for
// MissingIngredientKey's own constraints to actually run.
public record IgnoreMissingIngredientsRequest(@NotEmpty List<@Valid MissingIngredientKey> items) {
}
