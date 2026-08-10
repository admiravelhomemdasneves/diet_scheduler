package com.dietscheduler.backend.ws;

import com.dietscheduler.backend.auth.JwtService;
import com.dietscheduler.backend.household.HouseholdMemberRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Validates the app JWT (passed as ?token=... since WebSocket handshakes from mobile clients
 * can't reliably set custom Authorization headers on every platform) and household membership
 * before allowing the upgrade to succeed.
 */
@Component
public class HouseholdAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final HouseholdMemberRepository householdMemberRepository;

    public HouseholdAuthHandshakeInterceptor(JwtService jwtService, HouseholdMemberRepository householdMemberRepository) {
        this.jwtService = jwtService;
        this.householdMemberRepository = householdMemberRepository;
    }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                    @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        String path = request.getURI().getPath();
        String householdIdSegment = path.substring(path.lastIndexOf('/') + 1);

        UUID householdId;
        try {
            householdId = UUID.fromString(householdIdSegment);
        } catch (IllegalArgumentException e) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        String token = extractQueryParam(request.getURI().getQuery(), "token");
        if (token == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        UUID userId;
        try {
            userId = jwtService.parseUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (!householdMemberRepository.existsByHouseholdIdAndUserId(householdId, userId)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put("householdId", householdId);
        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // no-op
    }

    private String extractQueryParam(@Nullable String query, String key) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
