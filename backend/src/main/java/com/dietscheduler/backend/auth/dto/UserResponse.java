package com.dietscheduler.backend.auth.dto;

import com.dietscheduler.backend.user.User;

import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
