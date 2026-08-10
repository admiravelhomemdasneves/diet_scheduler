package com.dietscheduler.backend.preferences;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Household-level allergy list -- one-time-inherited from the union of member allergies, then independently editable per household. */
@Entity
@Table(name = "household_allergy", uniqueConstraints = @UniqueConstraint(columnNames = {"household_id", "allergy_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdAllergy {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "allergy_id", nullable = false)
    private UUID allergyId;
}
