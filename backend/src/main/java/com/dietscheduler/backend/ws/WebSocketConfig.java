package com.dietscheduler.backend.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PantryWebSocketHandler pantryWebSocketHandler;
    private final HouseholdAuthHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(PantryWebSocketHandler pantryWebSocketHandler, HouseholdAuthHandshakeInterceptor handshakeInterceptor) {
        this.pantryWebSocketHandler = pantryWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pantryWebSocketHandler, "/ws/households/*")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
