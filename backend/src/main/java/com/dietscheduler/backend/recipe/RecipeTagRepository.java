package com.dietscheduler.backend.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeTagRepository extends JpaRepository<RecipeTag, UUID> {
    List<RecipeTag> findByRecipeId(UUID recipeId);
    List<RecipeTag> findByRecipeIdIn(List<UUID> recipeIds);
    void deleteByRecipeId(UUID recipeId);
}
