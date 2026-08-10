package com.dietscheduler.backend.pantry;

import com.dietscheduler.backend.pantry.dto.CreatePantryItemRequest;
import com.dietscheduler.backend.pantry.dto.PantryItemResponse;
import com.dietscheduler.backend.pantry.dto.UpdatePantryItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class PantryController {

    private final PantryService pantryService;

    public PantryController(PantryService pantryService) {
        this.pantryService = pantryService;
    }

    @GetMapping("/households/{householdId}/pantry")
    public List<PantryItemResponse> list(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId) {
        return pantryService.list(householdId, userId);
    }

    @PostMapping("/households/{householdId}/pantry")
    @ResponseStatus(HttpStatus.CREATED)
    public PantryItemResponse create(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                      @Valid @RequestBody CreatePantryItemRequest request) {
        return pantryService.create(householdId, userId, request);
    }

    @PatchMapping("/pantry/{id}")
    public PantryItemResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                      @RequestBody UpdatePantryItemRequest request) {
        return pantryService.update(id, userId, request);
    }

    @DeleteMapping("/pantry/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        pantryService.delete(id, userId);
    }
}
