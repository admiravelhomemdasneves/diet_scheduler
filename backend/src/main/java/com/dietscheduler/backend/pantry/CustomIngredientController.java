package com.dietscheduler.backend.pantry;

import com.dietscheduler.backend.household.HouseholdService;
import com.dietscheduler.backend.pantry.dto.CustomIngredientRequest;
import com.dietscheduler.backend.pantry.dto.IngredientResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/households/{householdId}/custom-ingredients")
public class CustomIngredientController {

    private final IngredientService ingredientService;
    private final HouseholdService householdService;

    public CustomIngredientController(IngredientService ingredientService, HouseholdService householdService) {
        this.ingredientService = ingredientService;
        this.householdService = householdService;
    }

    @GetMapping
    public List<IngredientResponse> list(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId) {
        householdService.requireMembership(householdId, userId);
        return ingredientService.listCustom(householdId).stream().map(IngredientResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse create(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                      @Valid @RequestBody CustomIngredientRequest request) {
        householdService.requireMembership(householdId, userId);
        Ingredient ingredient = ingredientService.createCustom(householdId, request.name(), request.imageUrl(), request.category(),
                request.caloriesPer100g(), request.proteinPer100g(), request.carbsPer100g(), request.fatPer100g());
        return IngredientResponse.from(ingredient);
    }

    @PatchMapping("/{id}")
    public IngredientResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId, @PathVariable UUID id,
                                      @Valid @RequestBody CustomIngredientRequest request) {
        householdService.requireMembership(householdId, userId);
        Ingredient ingredient = ingredientService.updateCustom(householdId, id, request.name(), request.imageUrl(), request.category(),
                request.caloriesPer100g(), request.proteinPer100g(), request.carbsPer100g(), request.fatPer100g());
        return IngredientResponse.from(ingredient);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId, @PathVariable UUID id) {
        householdService.requireMembership(householdId, userId);
        ingredientService.deleteCustom(householdId, id);
    }
}
