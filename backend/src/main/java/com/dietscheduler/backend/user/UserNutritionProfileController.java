package com.dietscheduler.backend.user;

import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.user.dto.NutritionProfileResponse;
import com.dietscheduler.backend.user.dto.UpdateNutritionProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users/me/nutrition-profile")
public class UserNutritionProfileController {

    private final UserRepository userRepository;

    public UserNutritionProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public NutritionProfileResponse get(@AuthenticationPrincipal UUID userId) {
        return NutritionProfileResponse.from(findUser(userId));
    }

    @PatchMapping
    @Transactional
    public NutritionProfileResponse update(@AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateNutritionProfileRequest request) {
        User user = findUser(userId);
        if (request.gender() != null) user.setGender(request.gender());
        if (request.age() != null) user.setAge(request.age());
        if (request.weight() != null) user.setWeight(request.weight());
        if (request.height() != null) user.setHeight(request.height());
        if (request.calorieTarget() != null) user.setCalorieTarget(request.calorieTarget());
        if (request.proteinTargetGrams() != null) user.setProteinTargetGrams(request.proteinTargetGrams());
        if (request.carbsTargetGrams() != null) user.setCarbsTargetGrams(request.carbsTargetGrams());
        if (request.fatTargetGrams() != null) user.setFatTargetGrams(request.fatTargetGrams());
        if (request.weightGoalKg() != null) user.setWeightGoalKg(request.weightGoalKg());
        return NutritionProfileResponse.from(userRepository.save(user));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
