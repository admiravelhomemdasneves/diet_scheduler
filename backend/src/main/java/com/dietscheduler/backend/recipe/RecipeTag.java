package com.dietscheduler.backend.recipe;

import com.dietscheduler.backend.preferences.TasteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "recipe_tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeTag {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false)
    private TasteType tagType;

    @Column(nullable = false)
    private String value;
}
