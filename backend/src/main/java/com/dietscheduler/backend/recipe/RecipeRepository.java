package com.dietscheduler.backend.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    @Query("SELECT r FROM Recipe r WHERE r.isPrivate = false OR r.createdByUserId = :userId")
    List<Recipe> findAllVisibleTo(@Param("userId") UUID userId);

    Optional<Recipe> findFirstByNameIgnoreCaseAndCreatedByUserId(String name, UUID createdByUserId);
}
