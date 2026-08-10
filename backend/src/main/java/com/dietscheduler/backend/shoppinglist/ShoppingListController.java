package com.dietscheduler.backend.shoppinglist;

import com.dietscheduler.backend.shoppinglist.dto.AddMissingIngredientsRequest;
import com.dietscheduler.backend.shoppinglist.dto.CreateShoppingListItemRequest;
import com.dietscheduler.backend.shoppinglist.dto.IgnoreMissingIngredientsRequest;
import com.dietscheduler.backend.shoppinglist.dto.MissingIngredientResponse;
import com.dietscheduler.backend.shoppinglist.dto.ShoppingListItemResponse;
import com.dietscheduler.backend.shoppinglist.dto.UpdateShoppingListItemRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @GetMapping("/households/{householdId}/shopping-list")
    public List<ShoppingListItemResponse> list(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId) {
        return shoppingListService.list(householdId, userId);
    }

    @PostMapping("/households/{householdId}/shopping-list")
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingListItemResponse create(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                            @Valid @RequestBody CreateShoppingListItemRequest request) {
        return shoppingListService.create(householdId, userId, request);
    }

    @PostMapping("/households/{householdId}/shopping-list/generate")
    public List<ShoppingListItemResponse> generate(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return shoppingListService.generate(householdId, userId, from, to);
    }

    @PatchMapping("/shopping-list/{id}")
    public ShoppingListItemResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                            @Valid @RequestBody UpdateShoppingListItemRequest request) {
        return shoppingListService.update(id, userId, request);
    }

    @DeleteMapping("/shopping-list/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        shoppingListService.delete(id, userId);
    }

    @GetMapping("/households/{householdId}/shopping-list/missing-upcoming")
    public List<MissingIngredientResponse> missingUpcoming(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                                             @RequestParam int days) {
        return shoppingListService.getMissingUpcoming(householdId, userId, days);
    }

    @PostMapping("/households/{householdId}/shopping-list/missing-upcoming/ignore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ignoreMissing(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                               @Valid @RequestBody IgnoreMissingIngredientsRequest request) {
        shoppingListService.ignoreMissing(householdId, userId, request.items());
    }

    @PostMapping("/households/{householdId}/shopping-list/missing-upcoming/add")
    public List<ShoppingListItemResponse> addMissing(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                                       @Valid @RequestBody AddMissingIngredientsRequest request) {
        return shoppingListService.addMissingToList(householdId, userId, request.items());
    }
}
