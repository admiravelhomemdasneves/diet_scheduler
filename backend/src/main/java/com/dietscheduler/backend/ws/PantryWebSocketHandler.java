package com.dietscheduler.backend.ws;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

/** Broadcast-only channel for M0: clients receive pantry change events, no client->server messages are expected. */
@Component
public class PantryWebSocketHandler extends TextWebSocketHandler {

    private final HouseholdSocketRegistry registry;

    public PantryWebSocketHandler(HouseholdSocketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        registry.register(householdId(session), session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        registry.unregister(householdId(session), session);
    }

    private UUID householdId(WebSocketSession session) {
        return (UUID) session.getAttributes().get("householdId");
    }
}
