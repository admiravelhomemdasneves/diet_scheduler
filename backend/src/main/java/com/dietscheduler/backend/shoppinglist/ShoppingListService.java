package com.dietscheduler.backend.shoppinglist;

import com.dietscheduler.backend.common.DateRangeValidation;
import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.config.LimitsProperties;
import com.dietscheduler.backend.household.HouseholdService;
import com.dietscheduler.backend.mealplan.MealPlan;
import com.dietscheduler.backend.mealplan.MealPlanPortion;
import com.dietscheduler.backend.mealplan.MealPlanPortionRepository;
import com.dietscheduler.backend.mealplan.MealPlanRepository;
import com.dietscheduler.backend.pantry.Ingredient;
import com.dietscheduler.backend.pantry.IngredientRepository;
import com.dietscheduler.backend.pantry.PantryEvent;
import com.dietscheduler.backend.pantry.PantryItem;
import com.dietscheduler.backend.pantry.PantryItemRepository;
import com.dietscheduler.backend.pantry.PantryLocation;
import com.dietscheduler.backend.pantry.dto.PantryItemResponse;
import com.dietscheduler.backend.recipe.Recipe;
import com.dietscheduler.backend.recipe.RecipeIngredient;
import com.dietscheduler.backend.recipe.RecipeIngredientRepository;
import com.dietscheduler.backend.recipe.RecipeRepository;
import com.dietscheduler.backend.shoppinglist.dto.AddMissingIngredientItem;
import com.dietscheduler.backend.shoppinglist.dto.CreateShoppingListItemRequest;
import com.dietscheduler.backend.shoppinglist.dto.MissingIngredientKey;
import com.dietscheduler.backend.shoppinglist.dto.MissingIngredientResponse;
import com.dietscheduler.backend.shoppinglist.dto.ShoppingListItemResponse;
import com.dietscheduler.backend.shoppinglist.dto.UpdateShoppingListItemRequest;
import com.dietscheduler.backend.ws.HouseholdSocketRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShoppingListService {

    private final ShoppingListItemRepository shoppingListItemRepository;
    private final IngredientRepository ingredientRepository;
    private final HouseholdService householdService;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanPortionRepository mealPlanPortionRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final PantryItemRepository pantryItemRepository;
    private final IgnoredMissingIngredientRepository ignoredMissingIngredientRepository;
    private final HouseholdSocketRegistry socketRegistry;
    private final ObjectMapper objectMapper;
    private final LimitsProperties limits;

    public ShoppingListService(ShoppingListItemRepository shoppingListItemRepository, IngredientRepository ingredientRepository,
                                HouseholdService householdService, MealPlanRepository mealPlanRepository,
                                MealPlanPortionRepository mealPlanPortionRepository, RecipeRepository recipeRepository,
                                RecipeIngredientRepository recipeIngredientRepository, PantryItemRepository pantryItemRepository,
                                IgnoredMissingIngredientRepository ignoredMissingIngredientRepository,
                                HouseholdSocketRegistry socketRegistry, ObjectMapper objectMapper, LimitsProperties limits) {
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.ingredientRepository = ingredientRepository;
        this.householdService = householdService;
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanPortionRepository = mealPlanPortionRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.ignoredMissingIngredientRepository = ignoredMissingIngredientRepository;
        this.socketRegistry = socketRegistry;
        this.objectMapper = objectMapper;
        this.limits = limits;
    }

    public List<ShoppingListItemResponse> list(UUID householdId, UUID requesterUserId) {
        householdService.requireMembership(householdId, requesterUserId);
        return toResponses(shoppingListItemRepository.findByHouseholdId(householdId));
    }

    @Transactional
    public ShoppingListItemResponse create(UUID householdId, UUID requesterUserId, CreateShoppingListItemRequest request) {
        householdService.requireMembership(householdId, requesterUserId);
        Ingredient ingredient = resolveIngredient(request.ingredientId(), request.ingredientName());

        ShoppingListItem item = shoppingListItemRepository.save(ShoppingListItem.builder()
                .householdId(householdId)
                .ingredientId(ingredient.getId())
                .quantity(request.quantity())
                .unit(request.unit())
                .checked(false)
                .source(ShoppingListSource.MANUAL)
                .build());

        ShoppingListItemResponse response = ShoppingListItemResponse.from(item, ingredient);
        broadcastSingle(householdId, ShoppingListEvent.Type.SHOPPING_LIST_ITEM_CREATED, response);
        return response;
    }

    @Transactional
    public ShoppingListItemResponse update(UUID itemId, UUID requesterUserId, UpdateShoppingListItemRequest request) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Shopping list item not found"));
        householdService.requireMembership(item.getHouseholdId(), requesterUserId);

        // Only a false-to-true transition counts as "just bought" -- guards against re-adding to the
        // pantry on a redundant PATCH (e.g. re-sending checked=true for an already-checked item).
        boolean justChecked = Boolean.TRUE.equals(request.checked()) && !item.isChecked();
        if (request.checked() != null) {
            item.setChecked(request.checked());
        }
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        item = shoppingListItemRepository.save(item);

        Ingredient ingredient = ingredientRepository.findById(item.getIngredientId()).orElse(null);
        if (justChecked) {
            addToPantry(item, ingredient);
        }

        ShoppingListItemResponse response = ShoppingListItemResponse.from(item, ingredient);
        broadcastSingle(item.getHouseholdId(), ShoppingListEvent.Type.SHOPPING_LIST_ITEM_UPDATED, response);
        return response;
    }

    /**
     * Marking a shopping-list item bought adds it straight to the pantry (default location PANTRY,
     * since the shopping list has no location concept) -- merging into an existing pantry item for
     * the same ingredient+unit if one exists, rather than always inserting a new row, so repeatedly
     * buying the same staple accumulates quantity instead of fragmenting into duplicate rows.
     */
    private void addToPantry(ShoppingListItem item, Ingredient ingredient) {
        List<PantryItem> existing = pantryItemRepository.findByHouseholdIdAndIngredientIdAndUnit(
                item.getHouseholdId(), item.getIngredientId(), item.getUnit());

        PantryItem pantryItem;
        PantryEvent.Type eventType;
        if (!existing.isEmpty()) {
            pantryItem = existing.get(0);
            pantryItem.setQuantity(pantryItem.getQuantity().add(item.getQuantity()));
            pantryItem.setUpdatedAt(Instant.now());
            eventType = PantryEvent.Type.PANTRY_ITEM_UPDATED;
        } else {
            pantryItem = PantryItem.builder()
                    .householdId(item.getHouseholdId())
                    .ingredientId(item.getIngredientId())
                    .location(PantryLocation.PANTRY)
                    .quantity(item.getQuantity())
                    .unit(item.getUnit())
                    .build();
            eventType = PantryEvent.Type.PANTRY_ITEM_CREATED;
        }
        pantryItem = pantryItemRepository.save(pantryItem);

        try {
            socketRegistry.broadcast(item.getHouseholdId(),
                    objectMapper.writeValueAsString(new PantryEvent(eventType, PantryItemResponse.from(pantryItem, ingredient))));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize pantry event", e);
        }
    }

    @Transactional
    public void delete(UUID itemId, UUID requesterUserId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Shopping list item not found"));
        householdService.requireMembership(item.getHouseholdId(), requesterUserId);

        Ingredient ingredient = ingredientRepository.findById(item.getIngredientId()).orElse(null);
        ShoppingListItemResponse response = ShoppingListItemResponse.from(item, ingredient);

        shoppingListItemRepository.delete(item);
        broadcastSingle(item.getHouseholdId(), ShoppingListEvent.Type.SHOPPING_LIST_ITEM_DELETED, response);
    }

    @Transactional
    public List<ShoppingListItemResponse> generate(UUID householdId, UUID requesterUserId, LocalDate from, LocalDate to) {
        householdService.requireMembership(householdId, requesterUserId);
        DateRangeValidation.requireValidRange(from, to, limits.maxPlanRangeDays());

        Map<String, BigDecimal> needed = computeNeeded(householdId, from, to);
        Map<String, BigDecimal> available = computeAvailable(householdId);

        shoppingListItemRepository.deleteByHouseholdIdAndSource(householdId, ShoppingListSource.AUTO_GENERATED);

        for (Map.Entry<String, BigDecimal> entry : needed.entrySet()) {
            BigDecimal gap = entry.getValue().subtract(available.getOrDefault(entry.getKey(), BigDecimal.ZERO));
            if (gap.compareTo(BigDecimal.ZERO) > 0) {
                UUID ingredientId = UUID.fromString(splitKey(entry.getKey())[0]);
                String unit = splitKey(entry.getKey())[1];
                shoppingListItemRepository.save(ShoppingListItem.builder()
                        .householdId(householdId)
                        .ingredientId(ingredientId)
                        .quantity(gap)
                        .unit(unit)
                        .checked(false)
                        .source(ShoppingListSource.AUTO_GENERATED)
                        .build());
            }
        }

        List<ShoppingListItemResponse> all = toResponses(shoppingListItemRepository.findByHouseholdId(householdId));
        broadcastRegenerated(householdId, all);
        return all;
    }

    /**
     * Read-only preview of ingredient shortfalls for the household's upcoming (today .. today+days-1)
     * meal plans -- same needed-vs-available gap computation as {@link #generate}, but without the
     * side effect of replacing the AUTO_GENERATED shopping-list bucket, since this section supports
     * per-item ignore/add actions that a wholesale delete-and-recreate would fight with. Excludes any
     * ingredient+unit already on the shopping list (nothing to add twice) or already ignored for this
     * household.
     */
    public List<MissingIngredientResponse> getMissingUpcoming(UUID householdId, UUID requesterUserId, int days) {
        householdService.requireMembership(householdId, requesterUserId);
        DateRangeValidation.requireValidLookahead(days, limits.maxLookaheadDays());
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(Math.max(days, 1) - 1L);

        Map<String, BigDecimal> needed = computeNeeded(householdId, from, to);
        Map<String, BigDecimal> available = computeAvailable(householdId);

        Set<String> alreadyOnList = shoppingListItemRepository.findByHouseholdId(householdId).stream()
                .map(i -> i.getIngredientId() + "|" + i.getUnit())
                .collect(Collectors.toSet());
        Set<String> ignored = ignoredMissingIngredientRepository.findByHouseholdId(householdId).stream()
                .map(i -> i.getIngredientId() + "|" + i.getUnit())
                .collect(Collectors.toSet());

        Map<String, BigDecimal> gapsByKey = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : needed.entrySet()) {
            if (alreadyOnList.contains(entry.getKey()) || ignored.contains(entry.getKey())) {
                continue;
            }
            BigDecimal gap = entry.getValue().subtract(available.getOrDefault(entry.getKey(), BigDecimal.ZERO));
            if (gap.compareTo(BigDecimal.ZERO) > 0) {
                gapsByKey.put(entry.getKey(), gap);
            }
        }

        Map<UUID, Ingredient> ingredientsById = ingredientRepository.findAllById(
                        gapsByKey.keySet().stream().map(k -> UUID.fromString(splitKey(k)[0])).distinct().toList())
                .stream().collect(Collectors.toMap(Ingredient::getId, i -> i));

        List<MissingIngredientResponse> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : gapsByKey.entrySet()) {
            UUID ingredientId = UUID.fromString(splitKey(entry.getKey())[0]);
            String unit = splitKey(entry.getKey())[1];
            Ingredient ingredient = ingredientsById.get(ingredientId);
            result.add(new MissingIngredientResponse(ingredientId, ingredient != null ? ingredient.getName() : null, entry.getValue(), unit));
        }
        return result;
    }

    @Transactional
    public void ignoreMissing(UUID householdId, UUID requesterUserId, List<MissingIngredientKey> items) {
        householdService.requireMembership(householdId, requesterUserId);
        for (MissingIngredientKey item : items) {
            if (!ignoredMissingIngredientRepository.existsByHouseholdIdAndIngredientIdAndUnit(householdId, item.ingredientId(), item.unit())) {
                ignoredMissingIngredientRepository.save(IgnoredMissingIngredient.builder()
                        .householdId(householdId).ingredientId(item.ingredientId()).unit(item.unit()).build());
            }
        }
    }

    /** Adds each shortfall to the shopping list as a MANUAL item, merging into an existing MANUAL entry for the same ingredient+unit if present. */
    @Transactional
    public List<ShoppingListItemResponse> addMissingToList(UUID householdId, UUID requesterUserId, List<AddMissingIngredientItem> items) {
        householdService.requireMembership(householdId, requesterUserId);
        for (AddMissingIngredientItem item : items) {
            Optional<ShoppingListItem> existing = shoppingListItemRepository
                    .findByHouseholdIdAndIngredientIdAndUnitAndSource(householdId, item.ingredientId(), item.unit(), ShoppingListSource.MANUAL);
            if (existing.isPresent()) {
                ShoppingListItem it = existing.get();
                it.setQuantity(it.getQuantity().add(item.quantity()));
                shoppingListItemRepository.save(it);
            } else {
                shoppingListItemRepository.save(ShoppingListItem.builder()
                        .householdId(householdId).ingredientId(item.ingredientId())
                        .quantity(item.quantity()).unit(item.unit())
                        .checked(false).source(ShoppingListSource.MANUAL).build());
            }
        }
        List<ShoppingListItemResponse> all = toResponses(shoppingListItemRepository.findByHouseholdId(householdId));
        broadcastRegenerated(householdId, all);
        return all;
    }

    private Map<String, BigDecimal> computeNeeded(UUID householdId, LocalDate from, LocalDate to) {
        List<MealPlan> mealPlans = mealPlanRepository.findByHouseholdIdAndDateBetween(householdId, from, to);
        Map<String, BigDecimal> needed = new LinkedHashMap<>(); // key: ingredientId|unit

        for (MealPlan mealPlan : mealPlans) {
            Recipe recipe = recipeRepository.findById(mealPlan.getRecipeId()).orElse(null);
            if (recipe == null || recipe.getServings() == null || recipe.getServings() <= 0) {
                continue;
            }
            BigDecimal totalPortions = mealPlanPortionRepository.findByMealPlanId(mealPlan.getId()).stream()
                    .map(MealPlanPortion::getPortionMultiplier)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalPortions.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal scaleFactor = totalPortions.divide(BigDecimal.valueOf(recipe.getServings()), 6, RoundingMode.HALF_UP);

            for (RecipeIngredient ri : recipeIngredientRepository.findByRecipeId(recipe.getId())) {
                String key = ri.getIngredientId() + "|" + ri.getUnit();
                BigDecimal amount = ri.getQuantity().multiply(scaleFactor).setScale(3, RoundingMode.HALF_UP);
                needed.merge(key, amount, BigDecimal::add);
            }
        }
        return needed;
    }

    private Map<String, BigDecimal> computeAvailable(UUID householdId) {
        return pantryItemRepository.findByHouseholdId(householdId).stream()
                .collect(Collectors.groupingBy(p -> p.getIngredientId() + "|" + p.getUnit(),
                        Collectors.reducing(BigDecimal.ZERO, PantryItem::getQuantity, BigDecimal::add)));
    }

    private String[] splitKey(String key) {
        return key.split("\\|", 2);
    }

    private Ingredient resolveIngredient(UUID ingredientId, String ingredientName) {
        if (ingredientId != null) {
            return ingredientRepository.findById(ingredientId)
                    .orElseThrow(() -> new NotFoundException("Ingredient not found"));
        }
        if (!StringUtils.hasText(ingredientName)) {
            throw new IllegalArgumentException("Either ingredientId or ingredientName is required");
        }
        return ingredientRepository.findByNameIgnoreCaseAndHouseholdIdIsNull(ingredientName.trim())
                .orElseGet(() -> ingredientRepository.save(Ingredient.builder().name(ingredientName.trim()).build()));
    }

    private List<ShoppingListItemResponse> toResponses(List<ShoppingListItem> items) {
        Map<UUID, Ingredient> ingredientsById = ingredientRepository.findAllById(
                        items.stream().map(ShoppingListItem::getIngredientId).distinct().toList())
                .stream().collect(Collectors.toMap(Ingredient::getId, i -> i));
        return items.stream().map(i -> ShoppingListItemResponse.from(i, ingredientsById.get(i.getIngredientId()))).toList();
    }

    private void broadcastSingle(UUID householdId, ShoppingListEvent.Type type, ShoppingListItemResponse item) {
        broadcast(householdId, ShoppingListEvent.single(type, item));
    }

    private void broadcastRegenerated(UUID householdId, List<ShoppingListItemResponse> items) {
        broadcast(householdId, ShoppingListEvent.regenerated(items));
    }

    private void broadcast(UUID householdId, ShoppingListEvent event) {
        try {
            socketRegistry.broadcast(householdId, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize shopping list event", e);
        }
    }
}
