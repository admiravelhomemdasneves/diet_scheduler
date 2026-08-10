package com.dietscheduler.backend.user;

import com.dietscheduler.backend.user.dto.UnitSystemResponse;
import com.dietscheduler.backend.user.dto.UpdateUnitSystemRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users/me/unit-system")
public class UserUnitSystemController {

    private final UserRepository userRepository;

    public UserUnitSystemController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public UnitSystemResponse get(@AuthenticationPrincipal UUID userId) {
        return UnitSystemResponse.from(userRepository.findRequiredById(userId));
    }

    @PatchMapping
    @Transactional
    public UnitSystemResponse update(@AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateUnitSystemRequest request) {
        User user = userRepository.findRequiredById(userId);
        user.setUnitSystemDefault(request.unitSystem());
        return UnitSystemResponse.from(userRepository.save(user));
    }
}
