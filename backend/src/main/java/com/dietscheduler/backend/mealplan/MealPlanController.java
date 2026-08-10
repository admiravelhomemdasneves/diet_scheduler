package com.dietscheduler.backend.mealplan;

import com.dietscheduler.backend.common.RateLimiterService;
import com.dietscheduler.backend.config.RateLimitProperties;
import com.dietscheduler.backend.mealplan.dto.AutoGenerateScheduleRequest;
import com.dietscheduler.backend.mealplan.dto.CreateMealPlanRequest;
import com.dietscheduler.backend.mealplan.dto.DayNutritionSummaryResponse;
import com.dietscheduler.backend.mealplan.dto.MealPlanResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;

    public MealPlanController(MealPlanService mealPlanService, RateLimiterService rateLimiterService,
                               RateLimitProperties rateLimitProperties) {
        this.mealPlanService = mealPlanService;
        this.rateLimiterService = rateLimiterService;
        this.rateLimitProperties = rateLimitProperties;
    }

    @GetMapping("/households/{householdId}/meal-plan")
    public List<MealPlanResponse> list(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return mealPlanService.listMealPlans(householdId, userId, from, to);
    }

    @PostMapping("/households/{householdId}/meal-plan")
    @ResponseStatus(HttpStatus.CREATED)
    public MealPlanResponse create(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                    @Valid @RequestBody CreateMealPlanRequest request) {
        return mealPlanService.createMealPlan(householdId, userId, request);
    }

    @DeleteMapping("/meal-plan/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        mealPlanService.deleteMealPlan(id, userId);
    }

    @PostMapping("/meal-plan/{id}/confirm-cooked")
    public MealPlanResponse confirmCooked(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return mealPlanService.confirmCooked(id, userId);
    }

    @PostMapping("/households/{householdId}/meal-plan/auto-generate")
    public List<MealPlanResponse> autoGenerate(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                                @Valid @RequestBody AutoGenerateScheduleRequest request) {
        rateLimiterService.requireWithinLimit(userId + ":auto-generate", rateLimitProperties.autoGeneratePerMinute());
        return mealPlanService.autoGenerateSchedule(householdId, userId, request);
    }

    @GetMapping("/households/{householdId}/meal-plan/day-summary")
    public DayNutritionSummaryResponse daySummary(@AuthenticationPrincipal UUID userId, @PathVariable UUID householdId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return mealPlanService.getDayNutritionSummary(householdId, userId, date);
    }
}
