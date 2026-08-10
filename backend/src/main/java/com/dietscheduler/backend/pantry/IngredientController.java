package com.dietscheduler.backend.pantry;

import com.dietscheduler.backend.common.RateLimiterService;
import com.dietscheduler.backend.config.RateLimitProperties;
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
    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;

    public IngredientController(IngredientService ingredientService, HouseholdService householdService,
                                 RateLimiterService rateLimiterService, RateLimitProperties rateLimitProperties) {
        this.ingredientService = ingredientService;
        this.householdService = householdService;
        this.rateLimiterService = rateLimiterService;
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostMapping("/scan-barcode")
    public IngredientResponse scanBarcode(@AuthenticationPrincipal UUID userId, @Valid @RequestBody ScanBarcodeRequest request) {
        rateLimiterService.requireWithinLimit(userId + ":barcode-scan", rateLimitProperties.barcodeScanPerMinute());
        Ingredient ingredient = ingredientService.findOrCreateByBarcode(request.barcode());
        return IngredientResponse.from(ingredient);
    }

    @GetMapping("/ingredients/search")
    public List<IngredientSuggestion> search(@AuthenticationPrincipal UUID userId, @RequestParam String query,
                                              @RequestParam UUID householdId) {
        householdService.requireMembership(householdId, userId);
        rateLimiterService.requireWithinLimit(userId + ":external-recipe", rateLimitProperties.externalRecipePerMinute());
        return ingredientService.searchOnline(query, householdId);
    }

    @GetMapping("/ingredients/{id}")
    public IngredientResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return IngredientResponse.from(ingredientService.getById(id, userId));
    }
}
