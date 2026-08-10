package com.dietscheduler.backend.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, UUID> {
    List<RecipeIngredient> findByRecipeId(UUID recipeId);
    List<RecipeIngredient> findByRecipeIdIn(List<UUID> recipeIds);
    void deleteByRecipeId(UUID recipeId);
    boolean existsByIngredientId(UUID ingredientId);
}
