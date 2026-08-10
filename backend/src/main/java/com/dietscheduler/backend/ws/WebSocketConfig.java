package com.dietscheduler.backend.ws;

import com.dietscheduler.backend.config.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PantryWebSocketHandler pantryWebSocketHandler;
    private final HouseholdAuthHandshakeInterceptor handshakeInterceptor;
    private final CorsProperties corsProperties;

    public WebSocketConfig(PantryWebSocketHandler pantryWebSocketHandler, HouseholdAuthHandshakeInterceptor handshakeInterceptor,
                            CorsProperties corsProperties) {
        this.pantryWebSocketHandler = pantryWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // The real access-control boundary is HouseholdAuthHandshakeInterceptor (JWT + household
        // membership), not this. This only matters for browser clients, which send an Origin
        // header Spring checks against the pattern list below; the native mobile client sends no
        // Origin header at all, so an empty (DIETSCHEDULER_CORS_ORIGINS-driven) list here doesn't
        // affect it -- Spring only enforces this check when an Origin header is actually present.
        registry.addHandler(pantryWebSocketHandler, "/ws/households/*")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(corsProperties.allowedOriginList().toArray(new String[0]));
    }
}
