package com.dietscheduler.backend.household;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "household")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Household {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "invite_code", nullable = false, unique = true, length = 8)
    private String inviteCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Whether this household's allergy/taste lists have already been one-time-seeded from the union of its members' personal preferences. */
    @Column(name = "preferences_inherited", nullable = false, columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean preferencesInherited = false;
}
