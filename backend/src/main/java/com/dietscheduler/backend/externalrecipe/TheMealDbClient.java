package com.dietscheduler.backend.externalrecipe;

import com.dietscheduler.backend.config.HttpClientProperties;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Free, keyless public recipe API (test API key "1", per TheMealDB's own docs -- not a secret).
 * https://www.themealdb.com/api.php
 */
@Service
public class TheMealDbClient {

    private static final Logger log = LoggerFactory.getLogger(TheMealDbClient.class);

    private final RestClient restClient;

    public TheMealDbClient(HttpClientProperties httpClientProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(httpClientProperties.connectTimeoutMs());
        requestFactory.setReadTimeout(httpClientProperties.readTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl("https://www.themealdb.com/api/json/v1/1")
                .requestFactory(requestFactory)
                .build();
    }

    public List<Meal> search(String query) {
        try {
            MealDbResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search.php").queryParam("s", query).build())
                    .retrieve()
                    .body(MealDbResponse.class);
            return response == null || response.meals() == null ? List.of() : response.meals();
        } catch (RestClientException e) {
            // Matches OpenFoodFactsClient's graceful-degradation pattern: an upstream outage or
            // timeout degrades external recipe search/import to "found nothing" rather than a 500.
            log.warn("TheMealDB search failed for \"{}\": {}", query, e.getMessage());
            return List.of();
        }
    }

    public Optional<Meal> lookup(String externalId) {
        try {
            MealDbResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/lookup.php").queryParam("i", externalId).build())
                    .retrieve()
                    .body(MealDbResponse.class);
            if (response == null || response.meals() == null || response.meals().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(response.meals().get(0));
        } catch (RestClientException e) {
            log.warn("TheMealDB lookup failed for {}: {}", externalId, e.getMessage());
            return Optional.empty();
        }
    }

    private record MealDbResponse(List<Meal> meals) {
    }

    public static class Meal {
        @JsonProperty("idMeal")
        public String id;
        @JsonProperty("strMeal")
        public String name;
        @JsonProperty("strCategory")
        public String category;
        @JsonProperty("strArea")
        public String area;
        @JsonProperty("strInstructions")
        public String instructions;
        @JsonProperty("strMealThumb")
        public String thumbnailUrl;

        private final Map<String, String> extra = new HashMap<>();

        @JsonAnySetter
        public void setExtra(String key, Object value) {
            if (value != null) {
                extra.put(key, value.toString());
            }
        }

        /** The 20 numbered strIngredientN/strMeasureN pairs TheMealDB flattens into the response, non-blank only. */
        public List<IngredientMeasure> ingredientMeasures() {
            List<IngredientMeasure> result = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                String ingredient = extra.get("strIngredient" + i);
                if (ingredient == null || ingredient.isBlank()) {
                    continue;
                }
                String measure = extra.get("strMeasure" + i);
                result.add(new IngredientMeasure(ingredient.trim(), measure == null ? "" : measure.trim()));
            }
            return result;
        }
    }

    public record IngredientMeasure(String name, String measure) {
    }
}
