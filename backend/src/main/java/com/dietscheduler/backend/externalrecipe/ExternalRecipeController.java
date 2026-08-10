package com.dietscheduler.backend.externalrecipe;

import com.dietscheduler.backend.externalrecipe.dto.ExternalRecipeDetail;
import com.dietscheduler.backend.externalrecipe.dto.ExternalRecipeSummary;
import com.dietscheduler.backend.recipe.dto.RecipeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/external-recipes")
public class ExternalRecipeController {

    private final TheMealDbClient client;
    private final ExternalRecipeService externalRecipeService;

    public ExternalRecipeController(TheMealDbClient client, ExternalRecipeService externalRecipeService) {
        this.client = client;
        this.externalRecipeService = externalRecipeService;
    }

    @GetMapping("/search")
    public List<ExternalRecipeSummary> search(@RequestParam String query) {
        return client.search(query).stream().map(ExternalRecipeSummary::from).toList();
    }

    @GetMapping("/{externalId}")
    public ExternalRecipeDetail getDetail(@PathVariable String externalId) {
        return externalRecipeService.getDetail(externalId);
    }

    @PostMapping("/{externalId}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse favorite(@AuthenticationPrincipal UUID userId, @PathVariable String externalId) {
        return externalRecipeService.importAsFavorite(userId, externalId);
    }
}
