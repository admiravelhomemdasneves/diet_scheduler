package com.dietscheduler.backend.shoppinglist;

import com.dietscheduler.backend.auth.JwtService;
import com.dietscheduler.backend.household.Household;
import com.dietscheduler.backend.household.HouseholdMember;
import com.dietscheduler.backend.household.HouseholdMemberRepository;
import com.dietscheduler.backend.household.HouseholdRepository;
import com.dietscheduler.backend.household.HouseholdRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the Phase-1 fix to AddMissingIngredientsRequest/AddMissingIngredientItem:
 * {@code List<@Valid AddMissingIngredientItem>} is required for the item-level constraints
 * (@NotNull/@Positive/@DecimalMax on quantity) to actually run. Before that fix, Bean Validation
 * only checked that the list itself was non-empty and a negative quantity sailed straight through
 * to ShoppingListService.addToPantry, corrupting pantry stock. This asserts the 400 directly
 * against the real endpoint + real validation pipeline, not just the DTO in isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddMissingIngredientsValidationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    @Autowired
    private JwtService jwtService;

    private UUID householdId;
    private String memberToken;

    @BeforeEach
    void seedHouseholdWithMember() {
        UUID memberUserId = UUID.randomUUID();
        Household household = householdRepository.save(Household.builder()
                .name("Validation Test Household")
                .createdBy(memberUserId)
                .inviteCode("VALID" + UUID.randomUUID().toString().substring(0, 2).toUpperCase())
                .build());
        householdId = household.getId();

        householdMemberRepository.save(HouseholdMember.builder()
                .householdId(householdId)
                .userId(memberUserId)
                .role(HouseholdRole.OWNER)
                .build());

        memberToken = jwtService.issueToken(memberUserId, memberUserId + "@test.local");
    }

    private ResponseEntity<String> postAddMissing(Object body) {
        String url = "http://localhost:" + port + "/households/" + householdId + "/shopping-list/missing-upcoming/add";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(memberToken);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    @Test
    void negativeQuantityInNestedItemIsRejected() {
        Map<String, Object> item = Map.of(
                "ingredientId", UUID.randomUUID().toString(),
                "quantity", new BigDecimal("-5"),
                "unit", "g"
        );
        ResponseEntity<String> response = postAddMissing(Map.of("items", List.of(item)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void blankUnitInNestedItemIsRejected() {
        Map<String, Object> item = new java.util.HashMap<>();
        item.put("ingredientId", UUID.randomUUID().toString());
        item.put("quantity", new BigDecimal("5"));
        item.put("unit", "");
        ResponseEntity<String> response = postAddMissing(Map.of("items", List.of(item)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void emptyItemsListIsRejected() {
        ResponseEntity<String> response = postAddMissing(Map.of("items", List.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
