package com.dietscheduler.backend.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HouseholdSocketRegistry {

    private final Map<UUID, Set<WebSocketSession>> sessionsByHousehold = new ConcurrentHashMap<>();

    public void register(UUID householdId, WebSocketSession session) {
        sessionsByHousehold.computeIfAbsent(householdId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(UUID householdId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByHousehold.get(householdId);
        if (sessions != null) {
            sessions.remove(session);
        }
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
}
