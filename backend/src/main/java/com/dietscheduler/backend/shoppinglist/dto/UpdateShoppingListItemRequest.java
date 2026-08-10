package com.dietscheduler.backend.shoppinglist.dto;

import java.math.BigDecimal;

/** All fields optional; only non-null fields are applied (PATCH semantics). */
public record UpdateShoppingListItemRequest(Boolean checked, BigDecimal quantity) {
}
