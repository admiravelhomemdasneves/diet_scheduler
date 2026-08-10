package com.dietscheduler.backend.pantry;

import com.dietscheduler.backend.household.HouseholdService;
import com.dietscheduler.backend.pantry.dto.IngredientResponse;
import com.dietscheduler.backend.pantry.dto.IngredientSuggestion;
import com.dietscheduler.backend.pantry.dto.ScanBarcodeRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pantry")
public class IngredientController {

    private final IngredientService ingredientService;
    private final HouseholdService householdService;

    public IngredientController(IngredientService ingredientService, HouseholdService householdService) {
        this.ingredientService = ingredientService;
        this.householdService = householdService;
    }

    @PostMapping("/scan-barcode")
    public IngredientResponse scanBarcode(@Valid @RequestBody ScanBarcodeRequest request) {
        Ingredient ingredient = ingredientService.findOrCreateByBarcode(request.barcode());
        return IngredientResponse.from(ingredient);
    }

    @GetMapping("/ingredients/search")
    public List<IngredientSuggestion> search(@AuthenticationPrincipal UUID userId, @RequestParam String query,
                                              @RequestParam UUID householdId) {
        householdService.requireMembership(householdId, userId);
        return ingredientService.searchOnline(query, householdId);
    }

    @GetMapping("/ingredients/{id}")
    public IngredientResponse get(@PathVariable UUID id) {
        return IngredientResponse.from(ingredientService.getById(id));
    }
}
