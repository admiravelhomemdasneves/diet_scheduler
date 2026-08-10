package com.dietscheduler.backend.shoppinglist;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A household's persistent dismissal of one ingredient+unit from the "missing for future recipes" section. */
@Entity
@Table(name = "ignored_missing_ingredient", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"household_id", "ingredient_id", "unit"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IgnoredMissingIngredient {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "ingredient_id", nullable = false)
    private UUID ingredientId;

    @Column(nullable = false)
    private String unit;

    @Column(name = "ignored_at", nullable = false)
    @Builder.Default
    private Instant ignoredAt = Instant.now();
}
