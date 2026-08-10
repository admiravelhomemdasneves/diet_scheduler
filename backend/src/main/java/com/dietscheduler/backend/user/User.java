package com.dietscheduler.backend.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"auth_provider", "provider_subject_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    /** The provider's stable subject identifier (Google's "sub" claim). */
    @Column(name = "provider_subject_id", nullable = false)
    private String providerSubjectId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    // Optional nutrition-targeting fields (v2), nullable in v1.
    private String gender;
    private Integer age;
    private Double weight;
    private Double height;

    // v2: directly user-specified daily targets (no BMR/TDEE calculation) plus a target weight.
    @Column(name = "calorie_target")
    private Double calorieTarget;
    @Column(name = "protein_target_grams")
    private Double proteinTargetGrams;
    @Column(name = "carbs_target_grams")
    private Double carbsTargetGrams;
    @Column(name = "fat_target_grams")
    private Double fatTargetGrams;
    @Column(name = "weight_goal_kg")
    private Double weightGoalKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_system_default", nullable = false)
    @Builder.Default
    private UnitSystem unitSystemDefault = UnitSystem.METRIC;

    /** How many days ahead the "missing for future recipes" shopping-list section looks; null means the default (see ShoppingPreferencesResponse). */
    @Column(name = "missing_ingredients_lookahead_days")
    private Integer missingIngredientsLookaheadDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
