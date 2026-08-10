package com.dietscheduler.backend.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Small helpers for the "load a batch of related entities and index them by id" idiom that
 * appeared -- byte-for-byte identical apart from the entity type -- at over a dozen call sites
 * across RecipeService, MealPlanService, PantryService, ShoppingListService, PreferenceService
 * and HouseholdService, wherever a response needs to look up an Ingredient/User/Recipe/Taste by
 * the id stored on some other row (e.g. a PantryItem's ingredientId) without N+1 queries.
 */
public final class RepositoryUtils {

    private RepositoryUtils() {
    }

    /** {@code repository.findAllById(ids)} collected into a {@code Map<ID, T>} keyed by {@code idExtractor}. */
    public static <T, ID> Map<ID, T> findAllByIdAsMap(JpaRepository<T, ID> repository, Collection<ID> ids,
                                                        Function<T, ID> idExtractor) {
        return repository.findAllById(ids).stream().collect(Collectors.toMap(idExtractor, Function.identity()));
    }
}
