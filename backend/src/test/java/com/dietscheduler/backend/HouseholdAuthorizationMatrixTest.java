package com.dietscheduler.backend;

import com.dietscheduler.backend.auth.JwtService;
import com.dietscheduler.backend.household.Household;
import com.dietscheduler.backend.household.HouseholdMember;
import com.dietscheduler.backend.household.HouseholdMemberRepository;
import com.dietscheduler.backend.household.HouseholdRepository;
import com.dietscheduler.backend.household.HouseholdRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single test that would have caught the GET /pantry/ingredients/{id} IDOR (see
 * project-readiness notes): for every household-scoped GET endpoint, a user who is NOT a member
 * of the household must never get a 200 -- only 403 (or, for the one endpoint whose ownership
 * check lives one level down at the resource itself, 404). Runs against a real embedded server +
 * H2 database (see pom.xml's surefire environmentVariables for how the required-config fail-fast
 * checks are satisfied for the test JVM), exercising the actual Spring Security filter chain, not
 * a mocked one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HouseholdAuthorizationMatrixTest {

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

    private static UUID householdId;
    private static UUID memberUserId;
    private static UUID outsiderUserId;

    private static boolean seeded = false;

    @BeforeAll
    static void ensureSeeded() {
        // Actual seeding happens in seedOnce() below, run once per class via a guard rather than
        // a static @BeforeAll (which can't use injected @Autowired fields).
    }

    private void seedOnce() {
        if (seeded) {
            return;
        }
        memberUserId = UUID.randomUUID();
        outsiderUserId = UUID.randomUUID();

        Household household = householdRepository.save(Household.builder()
                .name("Authz Test Household")
                .createdBy(memberUserId)
                .inviteCode("AUTHZ" + UUID.randomUUID().toString().substring(0, 3).toUpperCase())
                .build());
        householdId = household.getId();

        householdMemberRepository.save(HouseholdMember.builder()
                .householdId(householdId)
                .userId(memberUserId)
                .role(HouseholdRole.OWNER)
                .build());

        seeded = true;
    }

    private String tokenFor(UUID userId) {
        return jwtService.issueToken(userId, userId + "@test.local");
    }

    /** (method, path template with {householdId} substituted at call time). GET-only and
     * side-effect-free by design, so the same seeded household can be reused across every case. */
    static Stream<Arguments> householdScopedGetEndpoints() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/households/{householdId}"),
                Arguments.of(HttpMethod.GET, "/households/{householdId}/pantry"),
                Arguments.of(HttpMethod.GET, "/households/{householdId}/meal-plan?from=" + today + "&to=" + today.plusDays(6)),
                Arguments.of(HttpMethod.GET, "/households/{householdId}/meal-plan/day-summary?date=" + today),
                Arguments.of(HttpMethod.GET, "/households/{householdId}/shopping-list"),
                Arguments.of(HttpMethod.GET, "/households/{householdId}/shopping-list/missing-upcoming?days=7"),
                Arguments.of(HttpMethod.GET, "/households/{householdId}/allergies"),
                Arguments.of(HttpMethod.GET, "/households/{householdId}/tastes"),
                Arguments.of(HttpMethod.GET, "/households/{householdId}/custom-ingredients"),
                Arguments.of(HttpMethod.POST, "/households/{householdId}/invite")
        );
    }

    @ParameterizedTest(name = "{0} {1} rejects a non-member")
    @MethodSource("householdScopedGetEndpoints")
    void nonMemberIsRejected(HttpMethod method, String pathTemplate) {
        seedOnce();
        String url = "http://localhost:" + port + pathTemplate.replace("{householdId}", householdId.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFor(outsiderUserId));
        ResponseEntity<String> response = restTemplate.exchange(url, method, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode())
                .as("non-member request to %s %s must not succeed", method, pathTemplate)
                .isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest(name = "{0} {1} succeeds for an actual member")
    @MethodSource("householdScopedGetEndpoints")
    void memberIsAllowed(HttpMethod method, String pathTemplate) {
        seedOnce();
        String url = "http://localhost:" + port + pathTemplate.replace("{householdId}", householdId.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFor(memberUserId));
        ResponseEntity<String> response = restTemplate.exchange(url, method, new HttpEntity<>(headers), String.class);

        // Sanity check on the positive case -- a test that only ever asserts "not 200" for
        // everyone would trivially pass even if every endpoint required auth incorrectly.
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("actual member request to %s %s should succeed, got %s: %s",
                        method, pathTemplate, response.getStatusCode(), response.getBody())
                .isTrue();
    }

    @ParameterizedTest(name = "{0} {1} rejects an unauthenticated caller")
    @MethodSource("householdScopedGetEndpoints")
    void unauthenticatedIsRejected(HttpMethod method, String pathTemplate) {
        seedOnce();
        String url = "http://localhost:" + port + pathTemplate.replace("{householdId}", householdId.toString());

        ResponseEntity<String> response = restTemplate.exchange(url, method, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
