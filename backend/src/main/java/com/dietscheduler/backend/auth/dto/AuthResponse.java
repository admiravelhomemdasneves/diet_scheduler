package com.dietscheduler.backend.auth.dto;

public record AuthResponse(String token, UserResponse user) {
}
