package com.dietscheduler.backend.auth;

import com.dietscheduler.backend.config.CorsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper,
                           CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/google", "/actuator/health").permitAll()
                        // WebSocket handshake auth (JWT via query param + household membership) is enforced
                        // in HouseholdAuthHandshakeInterceptor, not here.
                        .requestMatchers("/ws/**").permitAll()
                        // Recipe photos are plain static assets (like the external API thumbnails they sit
                        // alongside) -- Flutter's Image.network doesn't attach an Authorization header.
                        .requestMatchers("/recipe-images/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("timestamp", Instant.now().toString());
                    body.put("status", 401);
                    body.put("error", "Unauthorized");
                    body.put("message", "Missing or invalid bearer token");
                    objectMapper.writeValue(response.getWriter(), body);
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Driven by DIETSCHEDULER_CORS_ORIGINS (empty by default -- see CorsProperties). The
        // mobile app itself is unaffected either way: CORS is a browser-enforced mechanism and
        // the app doesn't send an Origin header, so this only matters for a future web client.
        configuration.setAllowedOriginPatterns(corsProperties.allowedOriginList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
