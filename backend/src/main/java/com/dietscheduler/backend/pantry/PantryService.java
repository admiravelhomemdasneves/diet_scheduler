package com.dietscheduler.backend.pantry;

import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.household.HouseholdService;
import com.dietscheduler.backend.pantry.dto.CreatePantryItemRequest;
import com.dietscheduler.backend.pantry.dto.PantryItemResponse;
import com.dietscheduler.backend.pantry.dto.UpdatePantryItemRequest;
import com.dietscheduler.backend.ws.HouseholdSocketRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PantryService {

    private final PantryItemRepository pantryItemRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientService ingredientService;
    private final HouseholdService householdService;
    private final HouseholdSocketRegistry socketRegistry;
    private final ObjectMapper objectMapper;

    public PantryService(PantryItemRepository pantryItemRepository, IngredientRepository ingredientRepository,
                          IngredientService ingredientService, HouseholdService householdService,
                          HouseholdSocketRegistry socketRegistry, ObjectMapper objectMapper) {
        this.pantryItemRepository = pantryItemRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientService = ingredientService;
        this.householdService = householdService;
        this.socketRegistry = socketRegistry;
        this.objectMapper = objectMapper;
    }

    public List<PantryItemResponse> list(UUID householdId, UUID requesterUserId) {
        householdService.requireMembership(householdId, requesterUserId);
        List<PantryItem> items = pantryItemRepository.findByHouseholdId(householdId);
        Map<UUID, Ingredient> ingredientsById = ingredientRepository.findAllById(
                        items.stream().map(PantryItem::getIngredientId).distinct().toList())
                .stream().collect(Collectors.toMap(Ingredient::getId, i -> i));
        return items.stream()
                .map(item -> PantryItemResponse.from(item, ingredientsById.get(item.getIngredientId())))
                .toList();
    }

    @Transactional
    public PantryItemResponse create(UUID householdId, UUID requesterUserId, CreatePantryItemRequest request) {
        householdService.requireMembership(householdId, requesterUserId);
        Ingredient ingredient = resolveIngredient(householdId, request);

        PantryItem item = PantryItem.builder()
                .householdId(householdId)
                .ingredientId(ingredient.getId())
                .location(request.location())
                .quantity(request.quantity())
                .unit(request.unit())
                .expirationDate(request.expirationDate())
                .build();
        item = pantryItemRepository.save(item);

        PantryItemResponse response = PantryItemResponse.from(item, ingredient);
        broadcast(householdId, PantryEvent.Type.PANTRY_ITEM_CREATED, response);
        return response;
    }

    @Transactional
    public PantryItemResponse update(UUID itemId, UUID requesterUserId, UpdatePantryItemRequest request) {
        PantryItem item = pantryItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Pantry item not found"));
        householdService.requireMembership(item.getHouseholdId(), requesterUserId);

        if (request.location() != null) {
            item.setLocation(request.location());
        }
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (StringUtils.hasText(request.unit())) {
            item.setUnit(request.unit());
        }
        if (request.expirationDate() != null) {
            item.setExpirationDate(request.expirationDate());
        }
        item.setUpdatedAt(Instant.now());

        Ingredient ingredient;
        if (StringUtils.hasText(request.ingredientName())) {
            ingredient = ingredientService.resolveEditableIngredient(item.getHouseholdId(), item.getIngredientId(),
                    request.ingredientName(), request.ingredientImageUrl(), request.ingredientCaloriesPer100g(),
                    request.ingredientProteinPer100g(), request.ingredientCarbsPer100g(), request.ingredientFatPer100g());
            item.setIngredientId(ingredient.getId());
        } else {
            ingredient = ingredientRepository.findById(item.getIngredientId()).orElse(null);
        }
        item = pantryItemRepository.save(item);

        PantryItemResponse response = PantryItemResponse.from(item, ingredient);
        broadcast(item.getHouseholdId(), PantryEvent.Type.PANTRY_ITEM_UPDATED, response);
        return response;
    }

    @Transactional
    public void delete(UUID itemId, UUID requesterUserId) {
        PantryItem item = pantryItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Pantry item not found"));
        householdService.requireMembership(item.getHouseholdId(), requesterUserId);

        Ingredient ingredient = ingredientRepository.findById(item.getIngredientId()).orElse(null);
        PantryItemResponse response = PantryItemResponse.from(item, ingredient);

        pantryItemRepository.delete(item);
        broadcast(item.getHouseholdId(), PantryEvent.Type.PANTRY_ITEM_DELETED, response);
    }

    private Ingredient resolveIngredient(UUID householdId, CreatePantryItemRequest request) {
        if (!StringUtils.hasText(request.ingredientName())) {
            throw new IllegalArgumentException("ingredientName is required");
        }
        if (request.ingredientId() != null) {
            // A known ingredient (barcode scan or a picked search suggestion) whose fields may
            // have been edited before submit -- reuses it unchanged, updates it in place if it's
            // already this household's custom ingredient, or forks a new custom one otherwise.
            return ingredientService.resolveEditableIngredient(householdId, request.ingredientId(),
                    request.ingredientName(), request.ingredientImageUrl(), request.ingredientCaloriesPer100g(),
                    request.ingredientProteinPer100g(), request.ingredientCarbsPer100g(), request.ingredientFatPer100g());
        }
        IngredientEnrichment enrichment = new IngredientEnrichment(
                request.ingredientBarcode(), request.ingredientImageUrl(), request.ingredientCategory(),
                request.ingredientCaloriesPer100g(), request.ingredientProteinPer100g(),
                request.ingredientCarbsPer100g(), request.ingredientFatPer100g());
        return ingredientService.findOrCreateByName(request.ingredientName().trim(), enrichment);
    }

    private void broadcast(UUID householdId, PantryEvent.Type type, PantryItemResponse item) {
        try {
            String json = objectMapper.writeValueAsString(new PantryEvent(type, item));
            socketRegistry.broadcast(householdId, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize pantry event", e);
        }
    }
}
