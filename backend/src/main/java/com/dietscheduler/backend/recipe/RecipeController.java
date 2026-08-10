package com.dietscheduler.backend.recipe;

import com.dietscheduler.backend.recipe.dto.CreateRecipeRequest;
import com.dietscheduler.backend.recipe.dto.RecipeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<RecipeResponse> list(@AuthenticationPrincipal UUID userId,
                                      @RequestParam(required = false) UUID householdId,
                                      @RequestParam(required = false) List<String> tags,
                                      @RequestParam(required = false, defaultValue = "NONE") StockFilter stockFilter) {
        return recipeService.listRecipes(userId, householdId, tags, stockFilter);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody CreateRecipeRequest request) {
        return recipeService.createRecipe(userId, request);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return recipeService.getRecipe(id, userId);
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                  @Valid @RequestBody CreateRecipeRequest request) {
        return recipeService.updateRecipe(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        recipeService.deleteRecipe(id, userId);
    }

    @PostMapping("/{id}/image")
    public RecipeResponse uploadImage(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                       @RequestParam("file") MultipartFile file) {
        return recipeService.updateImage(id, userId, file);
    }
}
