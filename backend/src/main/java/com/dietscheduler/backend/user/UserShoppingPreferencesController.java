package com.dietscheduler.backend.user;

import com.dietscheduler.backend.user.dto.ShoppingPreferencesResponse;
import com.dietscheduler.backend.user.dto.UpdateShoppingPreferencesRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users/me/shopping-preferences")
public class UserShoppingPreferencesController {

    private final UserRepository userRepository;

    public UserShoppingPreferencesController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ShoppingPreferencesResponse get(@AuthenticationPrincipal UUID userId) {
        return ShoppingPreferencesResponse.from(userRepository.findRequiredById(userId));
    }

    @PatchMapping
    @Transactional
    public ShoppingPreferencesResponse update(@AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateShoppingPreferencesRequest request) {
        User user = userRepository.findRequiredById(userId);
        user.setMissingIngredientsLookaheadDays(request.missingIngredientsLookaheadDays());
        return ShoppingPreferencesResponse.from(userRepository.save(user));
    }
}
