package com.dietscheduler.backend.pantry;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Global reference rows (household_id null) are shared Open Food Facts-backed catalog data.
 * A non-null household_id marks a "custom" ingredient owned by that household -- created either
 * explicitly or by forking a global/other-household ingredient the moment a user edits its
 * name/image/nutrition, so shared reference data is never mutated in place for everyone.
 */
@Entity
@Table(name = "ingredient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "household_id")
    private UUID householdId;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(name = "default_unit")
    private String defaultUnit;

    @Column(name = "calories_per_100g")
    private Double caloriesPer100g;
    @Column(name = "protein_per_100g")
    private Double proteinPer100g;
    @Column(name = "carbs_per_100g")
    private Double carbsPer100g;
    @Column(name = "fat_per_100g")
    private Double fatPer100g;

    @Column(unique = true)
    private String barcode;

    @Column(name = "image_url")
    private String imageUrl;
}
