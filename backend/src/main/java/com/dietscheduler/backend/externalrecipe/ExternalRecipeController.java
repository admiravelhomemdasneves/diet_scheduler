package com.dietscheduler.backend.externalrecipe;

import com.dietscheduler.backend.common.RateLimiterService;
import com.dietscheduler.backend.config.RateLimitProperties;
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
    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;

    public ExternalRecipeController(TheMealDbClient client, ExternalRecipeService externalRecipeService,
                                     RateLimiterService rateLimiterService, RateLimitProperties rateLimitProperties) {
        this.client = client;
        this.externalRecipeService = externalRecipeService;
        this.rateLimiterService = rateLimiterService;
        this.rateLimitProperties = rateLimitProperties;
    }

    @GetMapping("/search")
    public List<ExternalRecipeSummary> search(@AuthenticationPrincipal UUID userId, @RequestParam String query) {
        rateLimiterService.requireWithinLimit(userId + ":external-recipe", rateLimitProperties.externalRecipePerMinute());
        return client.search(query).stream().map(ExternalRecipeSummary::from).toList();
    }

    @GetMapping("/{externalId}")
    public ExternalRecipeDetail getDetail(@AuthenticationPrincipal UUID userId, @PathVariable String externalId) {
        // Shares the search bucket -- this is the more expensive of the two (fans out to up to 20
        // Open Food Facts lookups per call, see ExternalRecipeService.getDetail), so it's the one
        // that most needs capping, and one shared bucket is simpler to reason about than two.
        rateLimiterService.requireWithinLimit(userId + ":external-recipe", rateLimitProperties.externalRecipePerMinute());
        return externalRecipeService.getDetail(externalId);
    }

    @PostMapping("/{externalId}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse favorite(@AuthenticationPrincipal UUID userId, @PathVariable String externalId) {
        rateLimiterService.requireWithinLimit(userId + ":external-recipe", rateLimitProperties.externalRecipePerMinute());
        return externalRecipeService.importAsFavorite(userId, externalId);
    }
}
