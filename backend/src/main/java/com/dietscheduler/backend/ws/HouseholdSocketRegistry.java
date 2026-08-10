package com.dietscheduler.backend.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HouseholdSocketRegistry {

    private static final Logger log = LoggerFactory.getLogger(HouseholdSocketRegistry.class);

    /** Custom WS close code (application-defined range is 4000-4999 per RFC 6455) sent when a
     * member's own connection is force-closed because they just left/were removed from the
     * household, or the household itself was deleted. */
    public static final CloseStatus HOUSEHOLD_ACCESS_REVOKED = new CloseStatus(4001, "household access revoked");

    private final Map<UUID, Set<WebSocketSession>> sessionsByHousehold = new ConcurrentHashMap<>();

    public void register(UUID householdId, WebSocketSession session) {
        sessionsByHousehold.computeIfAbsent(householdId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(UUID householdId, WebSocketSession session) {
        // computeIfPresent, not a plain get-then-remove: once the last session for a household
        // disconnects, drop the now-empty Set from the map entirely rather than leaving it behind
        // forever -- otherwise this map only ever grows for the life of the process.
        sessionsByHousehold.computeIfPresent(householdId, (id, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void broadcast(UUID householdId, String jsonMessage) {
        Set<WebSocketSession> sessions = sessionsByHousehold.get(householdId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(jsonMessage);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                // WebSocketSession.sendMessage is not safe for concurrent calls from multiple threads.
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                // Best-effort broadcast; a failed send here just means that one client misses this update.
            }
        }
    }

    /** Force-closes every currently-open session this specific user has on this household's
     * channel -- called when they leave/are removed, so a departed member doesn't keep receiving
     * pantry/meal-plan/shopping-list events for a household they can no longer see in the app.
     * {@code afterConnectionClosed} fires for each closed session and calls {@link #unregister}
     * as normal, so no separate bookkeeping is needed here. */
    public void closeSessionsForUser(UUID householdId, UUID userId) {
        Set<WebSocketSession> sessions = sessionsByHousehold.get(householdId);
        if (sessions == null) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (userId.equals(session.getAttributes().get("userId"))) {
                closeQuietly(session);
            }
        }
    }

    /** Force-closes every session on this household's channel, for every member -- called when the
     * household itself is deleted. */
    public void closeAll(UUID householdId) {
        Set<WebSocketSession> sessions = sessionsByHousehold.get(householdId);
        if (sessions == null) {
            return;
        }
        // Iterate a snapshot: closing triggers afterConnectionClosed -> unregister, which mutates
        // this same underlying set concurrently with this loop.
        for (WebSocketSession session : Set.copyOf(sessions)) {
            closeQuietly(session);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(HOUSEHOLD_ACCESS_REVOKED);
            }
        } catch (IOException e) {
            log.debug("Failed to close WebSocket session {}: {}", session.getId(), e.getMessage());
        }
    }
}
