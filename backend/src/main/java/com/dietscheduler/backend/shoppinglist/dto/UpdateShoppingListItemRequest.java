package com.dietscheduler.backend.shoppinglist.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** All fields optional; only non-null fields are applied (PATCH semantics). */
public record UpdateShoppingListItemRequest(
        Boolean checked,
        @PositiveOrZero(message = "cannot be negative") @DecimalMax("1000000") BigDecimal quantity
) {
}
