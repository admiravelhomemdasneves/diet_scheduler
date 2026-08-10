package com.dietscheduler.backend.pantry;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/** Looks up and searches branded products against the public, keyless Open Food Facts database. */
@Service
public class OpenFoodFactsClient {

    private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsClient.class);
    private static final String FIELDS = "code,product_name,image_front_small_url,image_small_url,categories,quantity,nutriments";

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://world.openfoodfacts.org")
            .defaultHeader("User-Agent", "DietScheduler/0.1 (dev; contact: dev@dietscheduler.local)")
            .build();

    // Product-name text search lives on a separate subdomain (the newer Search-a-licious API);
    // the legacy world.openfoodfacts.org/cgi/search.pl endpoint has become unreliable.
    private final RestClient searchRestClient = RestClient.builder()
            .baseUrl("https://search.openfoodfacts.org")
            .defaultHeader("User-Agent", "DietScheduler/0.1 (dev; contact: dev@dietscheduler.local)")
            .build();

    public Optional<Product> lookup(String barcode) {
        try {
            OffResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/product/{barcode}.json")
                            .queryParam("fields", FIELDS)
                            .build(barcode))
                    .retrieve()
                    .body(OffResponse.class);

            if (response == null || response.status() != 1 || response.product() == null) {
                return Optional.empty();
            }
            return Optional.of(response.product());
        } catch (RestClientException e) {
            log.warn("Open Food Facts barcode lookup failed for {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    /** Free-text product search, used both for the ingredient-picker dropdown and auto-enriching custom names. */
    public List<Product> search(String query) {
        try {
            SearchResponse response = searchRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("page_size", "10")
                            .queryParam("fields", FIELDS)
                            .build())
                    .retrieve()
                    .body(SearchResponse.class);

            if (response == null || response.hits() == null) {
                return List.of();
            }
            return response.hits().stream()
                    .filter(p -> p.productName() != null && !p.productName().isBlank())
                    .toList();
        } catch (RestClientException e) {
            log.warn("Open Food Facts search failed for \"{}\": {}", query, e.getMessage());
            return List.of();
        }
    }

    private record OffResponse(int status, Product product) {
    }

    private record SearchResponse(List<Product> hits) {
    }

    public record Product(
            String code,
            @JsonProperty("product_name") String productName,
            @JsonProperty("image_front_small_url") String imageFrontSmallUrl,
            @JsonProperty("image_small_url") String imageSmallUrl,
            String categories,
            String quantity,
            Nutriments nutriments) {

        public String imageUrl() {
            return imageFrontSmallUrl != null ? imageFrontSmallUrl : imageSmallUrl;
        }
    }

    public record Nutriments(
            @JsonProperty("energy-kcal_100g") Double energyKcal100g,
            @JsonProperty("proteins_100g") Double proteins100g,
            @JsonProperty("carbohydrates_100g") Double carbohydrates100g,
            @JsonProperty("fat_100g") Double fat100g) {
    }
}
