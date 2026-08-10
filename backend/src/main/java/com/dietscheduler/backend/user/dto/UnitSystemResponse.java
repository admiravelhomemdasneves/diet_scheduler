package com.dietscheduler.backend.user.dto;

import com.dietscheduler.backend.user.User;
import com.dietscheduler.backend.user.UnitSystem;

public record UnitSystemResponse(UnitSystem unitSystem) {
    public static UnitSystemResponse from(User u) {
        return new UnitSystemResponse(u.getUnitSystemDefault());
    }
}
