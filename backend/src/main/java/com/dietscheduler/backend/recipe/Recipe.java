package com.dietscheduler.backend.recipe;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Globally visible by default. is_private is an opt-in exception scoped to the individual
 * creator (not their household) -- null createdByUserId means seeded/global, never editable via API.
 */
@Entity
@Table(name = "recipe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipeCategory category;

    @Column(columnDefinition = "text")
    private String instructions;

    @Column(nullable = false)
    private Integer servings;

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    @Column(name = "cook_time_minutes")
    private Integer cookTimeMinutes;

    @Column(name = "calories_per_serving")
    private Double caloriesPerServing;
    @Column(name = "protein_per_serving")
    private Double proteinPerServing;
    @Column(name = "carbs_per_serving")
    private Double carbsPerServing;
    @Column(name = "fat_per_serving")
    private Double fatPerServing;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    /** Either an absolute URL (e.g. an imported recipe's source thumbnail) or a relative /recipe-images/... path we serve. */
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private boolean isPrivate = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
