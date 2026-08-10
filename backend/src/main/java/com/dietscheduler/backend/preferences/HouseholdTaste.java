package com.dietscheduler.backend.preferences;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Household-level overrides/additions on top of the union of member tastes. */
@Entity
@Table(name = "household_taste", uniqueConstraints = @UniqueConstraint(columnNames = {"household_id", "taste_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdTaste {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "taste_id", nullable = false)
    private UUID tasteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TastePreference preference;
}
