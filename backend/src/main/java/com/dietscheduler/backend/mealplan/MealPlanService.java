package com.dietscheduler.backend.mealplan;

import com.dietscheduler.backend.common.ConflictException;
import com.dietscheduler.backend.common.DateRangeValidation;
import com.dietscheduler.backend.common.ForbiddenException;
import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.common.RepositoryUtils;
import com.dietscheduler.backend.config.LimitsProperties;
import com.dietscheduler.backend.household.HouseholdMember;
import com.dietscheduler.backend.household.HouseholdMemberRepository;
import com.dietscheduler.backend.household.HouseholdService;
import com.dietscheduler.backend.mealplan.dto.AutoGenerateScheduleRequest;
import com.dietscheduler.backend.mealplan.dto.CreateMealPlanRequest;
import com.dietscheduler.backend.mealplan.dto.DayNutritionSummaryResponse;
import com.dietscheduler.backend.mealplan.dto.MealPlanResponse;
import com.dietscheduler.backend.mealplan.dto.PortionRequest;
import com.dietscheduler.backend.mealplan.dto.PortionResponse;
import com.dietscheduler.backend.pantry.PantryItem;
import com.dietscheduler.backend.pantry.PantryItemRepository;
import com.dietscheduler.backend.pantry.PantryService;
import com.dietscheduler.backend.pantry.dto.UpdatePantryItemRequest;
import com.dietscheduler.backend.recipe.Recipe;
import com.dietscheduler.backend.recipe.RecipeIngredient;
import com.dietscheduler.backend.recipe.RecipeIngredientRepository;
import com.dietscheduler.backend.recipe.RecipeRepository;
import com.dietscheduler.backend.recipe.RecipeService;
import com.dietscheduler.backend.recipe.StockFilter;
import com.dietscheduler.backend.recipe.dto.RecipeResponse;
import com.dietscheduler.backend.user.User;
import com.dietscheduler.backend.user.UserRepository;
import com.dietscheduler.backend.ws.HouseholdSocketRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanPortionRepository mealPlanPortionRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UserRepository userRepository;
    private final HouseholdService householdService;
    private final PantryItemRepository pantryItemRepository;
    private final PantryService pantryService;
    private final RecipeService recipeService;
    private final HouseholdSocketRegistry socketRegistry;
    private final LimitsProperties limits;

    public MealPlanService(MealPlanRepository mealPlanRepository, MealPlanPortionRepository mealPlanPortionRepository,
                            RecipeRepository recipeRepository, RecipeIngredientRepository recipeIngredientRepository,
                            HouseholdMemberRepository householdMemberRepository,
                            UserRepository userRepository, HouseholdService householdService,
                            PantryItemRepository pantryItemRepository, PantryService pantryService,
                            RecipeService recipeService,
                            HouseholdSocketRegistry socketRegistry, LimitsProperties limits) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanPortionRepository = mealPlanPortionRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.userRepository = userRepository;
        this.householdService = householdService;
        this.pantryItemRepository = pantryItemRepository;
        this.pantryService = pantryService;
        this.recipeService = recipeService;
        this.socketRegistry = socketRegistry;
        this.limits = limits;
    }

    public List<MealPlanResponse> listMealPlans(UUID householdId, UUID requesterUserId, LocalDate from, LocalDate to) {
        householdService.requireMembership(householdId, requesterUserId);
        DateRangeValidation.requireValidRange(from, to, limits.maxPlanRangeDays());
        List<MealPlan> mealPlans = mealPlanRepository.findByHouseholdIdAndDateBetween(householdId, from, to);
        if (mealPlans.isEmpty()) {
            return List.of();
        }

        List<UUID> mealPlanIds = mealPlans.stream().map(MealPlan::getId).toList();
        Map<UUID, List<MealPlanPortion>> portionsByMealPlan = mealPlanPortionRepository.findByMealPlanIdIn(mealPlanIds)
                .stream().collect(Collectors.groupingBy(MealPlanPortion::getMealPlanId));
        Map<UUID, Recipe> recipesById = RepositoryUtils.findAllByIdAsMap(recipeRepository,
                mealPlans.stream().map(MealPlan::getRecipeId).distinct().toList(), Recipe::getId);
        Map<UUID, User> usersById = RepositoryUtils.findAllByIdAsMap(userRepository,
                portionsByMealPlan.values().stream().flatMap(List::stream).map(MealPlanPortion::getUserId).distinct().toList(),
                User::getId);

        return mealPlans.stream()
                .map(mp -> toResponse(mp, recipesById.get(mp.getRecipeId()),
                        portionsByMealPlan.getOrDefault(mp.getId(), List.of()), usersById))
                .toList();
    }

    @Transactional
    public MealPlanResponse createMealPlan(UUID householdId, UUID requesterUserId, CreateMealPlanRequest request) {
        householdService.requireMembership(householdId, requesterUserId);
        Recipe recipe = recipeRepository.findById(request.recipeId())
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        // Mirrors RecipeService.getRecipe's visibility check -- without it, any household member
        // could assign another user's private recipe to a meal slot by id, and its name/timing
        // would then be visible to the whole household via listMealPlans/the WS broadcast below.
        if (recipe.isPrivate() && !Objects.equals(recipe.getCreatedByUserId(), requesterUserId)) {
            throw new ForbiddenException("This recipe is private");
        }

        List<UUID> memberIds = householdMemberRepository.findByHouseholdId(householdId).stream()
                .map(m -> m.getUserId()).toList();

        List<PortionRequest> portionRequests = (request.portions() == null || request.portions().isEmpty())
                ? List.of(new PortionRequest(requesterUserId, BigDecimal.ONE))
                : request.portions();

        for (PortionRequest p : portionRequests) {
            if (!memberIds.contains(p.userId())) {
                throw new IllegalArgumentException("Portion assigned to a user who is not a household member");
            }
        }

        return createInternal(householdId, request.date(), request.mealType(), recipe, portionRequests);
    }

    private MealPlanResponse createInternal(UUID householdId, LocalDate date, MealType mealType, Recipe recipe,
                                              List<PortionRequest> portionRequests) {
        MealPlan mealPlan = mealPlanRepository.save(MealPlan.builder()
                .householdId(householdId)
                .date(date)
                .mealType(mealType)
                .recipeId(recipe.getId())
                .build());

        List<MealPlanPortion> portions = portionRequests.stream()
                .map(p -> mealPlanPortionRepository.save(MealPlanPortion.builder()
                        .mealPlanId(mealPlan.getId())
                        .userId(p.userId())
                        .portionMultiplier(p.portionMultiplier() != null ? p.portionMultiplier() : BigDecimal.ONE)
                        .build()))
                .toList();

        Map<UUID, User> usersById = RepositoryUtils.findAllByIdAsMap(userRepository,
                portions.stream().map(MealPlanPortion::getUserId).distinct().toList(), User::getId);

        MealPlanResponse response = toResponse(mealPlan, recipe, portions, usersById);
        broadcast(householdId, MealPlanEvent.Type.MEAL_PLAN_CREATED, response);
        return response;
    }

    @Transactional
    public List<MealPlanResponse> autoGenerateSchedule(UUID householdId, UUID requesterUserId, AutoGenerateScheduleRequest request) {
        householdService.requireMembership(householdId, requesterUserId);
        DateRangeValidation.requireValidRange(request.from(), request.to(), limits.maxPlanRangeDays());

        List<MealType> mealTypes = (request.mealTypes() == null || request.mealTypes().isEmpty())
                ? List.of(MealType.values())
                : request.mealTypes();
        StockFilter stockFilter = request.stockFilter() != null ? request.stockFilter() : StockFilter.NONE;
        int avoidRepeatDays = request.avoidRepeatDays() != null ? request.avoidRepeatDays() : 7;
        boolean targetNutrition = Boolean.TRUE.equals(request.targetNutrition());

        List<RecipeResponse> pool = recipeService.listRecipes(requesterUserId, householdId, null, stockFilter);
        if (pool.isEmpty()) {
            return List.of();
        }

        // Seed rotation history from meal plans already on the books, including a lookback window
        // before `from` so a recipe eaten just before the auto-fill range still counts as "recent".
        // Deliberately computed BEFORE any regenerate-deletion below, so a slot being regenerated
        // still counts its old (about-to-be-replaced) recipe as "recent" and tends to get picked a
        // different one, rather than immediately re-picking what was just there.
        LocalDate historyStart = request.from().minusDays(avoidRepeatDays);
        Map<UUID, LocalDate> lastUsed = new HashMap<>();
        for (MealPlan mp : mealPlanRepository.findByHouseholdIdAndDateBetween(householdId, historyStart, request.to())) {
            lastUsed.merge(mp.getRecipeId(), mp.getDate(), (a, b) -> a.isAfter(b) ? a : b);
        }

        if (Boolean.TRUE.equals(request.regenerate())) {
            deleteMealPlansForRegenerate(householdId, request.from(), request.to(), mealTypes);
        }

        Set<String> occupied = mealPlanRepository.findByHouseholdIdAndDateBetween(householdId, request.from(), request.to())
                .stream().map(mp -> mp.getDate() + "|" + mp.getMealType()).collect(Collectors.toSet());

        List<HouseholdMember> members = householdMemberRepository.findByHouseholdId(householdId);
        List<PortionRequest> defaultPortions = members.stream()
                .map(m -> new PortionRequest(m.getUserId(), BigDecimal.ONE))
                .toList();

        // Only members who've actually set a calorie target participate in nutrition scoring.
        Map<UUID, User> targetedUsers = new HashMap<>();
        if (targetNutrition) {
            for (User u : userRepository.findAllById(members.stream().map(HouseholdMember::getUserId).toList())) {
                if (u.getCalorieTarget() != null) {
                    targetedUsers.put(u.getId(), u);
                }
            }
        }

        List<MealPlanResponse> created = new ArrayList<>();
        for (LocalDate date = request.from(); !date.isAfter(request.to()); date = date.plusDays(1)) {
            Map<UUID, double[]> consumedToday = targetedUsers.isEmpty()
                    ? Map.of()
                    : initConsumedForDate(householdId, date, targetedUsers.keySet());

            for (MealType mealType : mealTypes) {
                if (occupied.contains(date + "|" + mealType)) {
                    continue;
                }

                RecipeResponse chosen = targetedUsers.isEmpty()
                        ? pickRecipe(pool, lastUsed, date, avoidRepeatDays)
                        : pickRecipeForNutrition(pool, lastUsed, date, avoidRepeatDays, targetedUsers, consumedToday);
                Recipe recipe = chosen != null ? recipeRepository.findById(chosen.id()).orElse(null) : null;
                if (recipe == null) {
                    continue;
                }
                created.add(createInternal(householdId, date, mealType, recipe, defaultPortions));
                lastUsed.put(recipe.getId(), date);

                for (UUID uid : targetedUsers.keySet()) {
                    double[] c = consumedToday.computeIfAbsent(uid, k -> new double[4]);
                    c[0] += nz(chosen.caloriesPerServing());
                    c[1] += nz(chosen.proteinPerServing());
                    c[2] += nz(chosen.carbsPerServing());
                    c[3] += nz(chosen.fatPerServing());
                }
            }
        }
        return created;
    }

    /** Deletes+broadcasts-away every existing meal plan in [from,to] matching mealTypes, so autoGenerateSchedule's regenerate path starts from a clean slate. */
    private void deleteMealPlansForRegenerate(UUID householdId, LocalDate from, LocalDate to, List<MealType> mealTypes) {
        List<MealPlan> existing = mealPlanRepository.findByHouseholdIdAndDateBetween(householdId, from, to).stream()
                .filter(mp -> mealTypes.contains(mp.getMealType()))
                .toList();
        if (existing.isEmpty()) {
            return;
        }

        List<UUID> ids = existing.stream().map(MealPlan::getId).toList();
        Map<UUID, List<MealPlanPortion>> portionsByMealPlan = mealPlanPortionRepository.findByMealPlanIdIn(ids)
                .stream().collect(Collectors.groupingBy(MealPlanPortion::getMealPlanId));
        Map<UUID, Recipe> recipesById = RepositoryUtils.findAllByIdAsMap(recipeRepository,
                existing.stream().map(MealPlan::getRecipeId).distinct().toList(), Recipe::getId);
        Map<UUID, User> usersById = RepositoryUtils.findAllByIdAsMap(userRepository,
                portionsByMealPlan.values().stream().flatMap(List::stream).map(MealPlanPortion::getUserId).distinct().toList(),
                User::getId);

        for (MealPlan mp : existing) {
            MealPlanResponse response = toResponse(mp, recipesById.get(mp.getRecipeId()),
                    portionsByMealPlan.getOrDefault(mp.getId(), List.of()), usersById);
            mealPlanPortionRepository.deleteByMealPlanId(mp.getId());
            mealPlanRepository.delete(mp);
            broadcast(householdId, MealPlanEvent.Type.MEAL_PLAN_DELETED, response);
        }
    }

    /**
     * Sums the requesting user's own nutrition for `date` -- each meal plan's recipe nutrition
     * (author-entered if present, else the same ingredient-based estimate RecipeService computes),
     * scaled by the requester's own portion on that meal plan (0 if they have no portion on it,
     * i.e. that meal isn't theirs) -- and compares the totals against their nutrition-profile
     * targets. Targets/deltas for a macro are null if the user hasn't set that target.
     */
    public DayNutritionSummaryResponse getDayNutritionSummary(UUID householdId, UUID requesterUserId, LocalDate date) {
        householdService.requireMembership(householdId, requesterUserId);
        List<MealPlan> plans = mealPlanRepository.findByHouseholdIdAndDateBetween(householdId, date, date);

        double calories = 0, protein = 0, carbs = 0, fat = 0;
        boolean incomplete = false;
        for (MealPlan mp : plans) {
            MealPlanPortion myPortion = mealPlanPortionRepository.findByMealPlanId(mp.getId()).stream()
                    .filter(p -> p.getUserId().equals(requesterUserId))
                    .findFirst().orElse(null);
            if (myPortion == null) {
                continue;
            }
            RecipeResponse recipe;
            try {
                recipe = recipeService.getRecipe(mp.getRecipeId(), requesterUserId);
            } catch (RuntimeException e) {
                incomplete = true;
                continue;
            }
            Double cal = recipe.caloriesPerServing() != null ? recipe.caloriesPerServing() : recipe.estimatedCaloriesPerServing();
            if (cal == null) {
                incomplete = true;
                continue;
            }
            Double pro = recipe.proteinPerServing() != null ? recipe.proteinPerServing() : recipe.estimatedProteinPerServing();
            Double carb = recipe.carbsPerServing() != null ? recipe.carbsPerServing() : recipe.estimatedCarbsPerServing();
            Double f = recipe.fatPerServing() != null ? recipe.fatPerServing() : recipe.estimatedFatPerServing();

            double mult = myPortion.getPortionMultiplier() != null ? myPortion.getPortionMultiplier().doubleValue() : 1.0;
            calories += cal * mult;
            if (pro != null) protein += pro * mult; else incomplete = true;
            if (carb != null) carbs += carb * mult; else incomplete = true;
            if (f != null) fat += f * mult; else incomplete = true;
        }

        User user = userRepository.findById(requesterUserId).orElse(null);
        Double calorieTarget = user != null ? user.getCalorieTarget() : null;
        Double proteinTarget = user != null ? user.getProteinTargetGrams() : null;
        Double carbsTarget = user != null ? user.getCarbsTargetGrams() : null;
        Double fatTarget = user != null ? user.getFatTargetGrams() : null;

        return new DayNutritionSummaryResponse(
                calories, protein, carbs, fat,
                calorieTarget, proteinTarget, carbsTarget, fatTarget,
                calorieTarget != null ? calories - calorieTarget : null,
                proteinTarget != null ? protein - proteinTarget : null,
                carbsTarget != null ? carbs - carbsTarget : null,
                fatTarget != null ? fat - fatTarget : null,
                incomplete);
    }

    /** Sums nutrition already committed for `date` (pre-existing meal plans) per targeted member, scaled by their portion. */
    private Map<UUID, double[]> initConsumedForDate(UUID householdId, LocalDate date, Set<UUID> targetedUserIds) {
        Map<UUID, double[]> consumed = new HashMap<>();
        for (MealPlan mp : mealPlanRepository.findByHouseholdIdAndDateBetween(householdId, date, date)) {
            Recipe recipe = recipeRepository.findById(mp.getRecipeId()).orElse(null);
            if (recipe == null) {
                continue;
            }
            for (MealPlanPortion p : mealPlanPortionRepository.findByMealPlanId(mp.getId())) {
                if (!targetedUserIds.contains(p.getUserId())) {
                    continue;
                }
                double mult = p.getPortionMultiplier() != null ? p.getPortionMultiplier().doubleValue() : 1.0;
                double[] c = consumed.computeIfAbsent(p.getUserId(), k -> new double[4]);
                c[0] += nz(recipe.getCaloriesPerServing()) * mult;
                c[1] += nz(recipe.getProteinPerServing()) * mult;
                c[2] += nz(recipe.getCarbsPerServing()) * mult;
                c[3] += nz(recipe.getFatPerServing()) * mult;
            }
        }
        return consumed;
    }

    /**
     * Among rotation-eligible candidates (falling back to the whole pool if none are eligible), picks
     * the one whose per-serving nutrition best closes the average remaining-budget gap across targeted
     * members -- the greedy heuristic described in the project plan. Candidates without any nutrition
     * data can't be scored and are skipped; if none qualify, falls back to plain rotation.
     */
    private RecipeResponse pickRecipeForNutrition(List<RecipeResponse> pool, Map<UUID, LocalDate> lastUsed, LocalDate date,
                                                   int avoidRepeatDays, Map<UUID, User> targetedUsers,
                                                   Map<UUID, double[]> consumedToday) {
        List<RecipeResponse> candidates = rotationEligibleCandidates(pool, lastUsed, date, avoidRepeatDays);

        RecipeResponse best = null;
        double bestScore = Double.MAX_VALUE;
        for (RecipeResponse candidate : candidates) {
            if (candidate.caloriesPerServing() == null) {
                continue;
            }
            double score = 0;
            for (User user : targetedUsers.values()) {
                double[] consumed = consumedToday.getOrDefault(user.getId(), new double[4]);
                double remainingCalories = nz(user.getCalorieTarget()) - consumed[0];
                double remainingProtein = nz(user.getProteinTargetGrams()) - consumed[1];
                double remainingCarbs = nz(user.getCarbsTargetGrams()) - consumed[2];
                double remainingFat = nz(user.getFatTargetGrams()) - consumed[3];
                // Macros are weighted by their calorie-per-gram so a gram of protein/carbs/fat contributes
                // roughly comparably to a kcal of deviation, instead of being dwarfed by the larger calorie numbers.
                score += Math.abs(remainingCalories - nz(candidate.caloriesPerServing()))
                        + Math.abs(remainingProtein - nz(candidate.proteinPerServing())) * 4
                        + Math.abs(remainingCarbs - nz(candidate.carbsPerServing())) * 4
                        + Math.abs(remainingFat - nz(candidate.fatPerServing())) * 9;
            }
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best != null ? best : pickRecipe(pool, lastUsed, date, avoidRepeatDays);
    }

    private double nz(Double d) {
        return d != null ? d : 0.0;
    }

    /**
     * Pool entries whose last use is outside the avoidRepeatDays cooldown (falling back to the
     * whole pool if that leaves nothing) -- was duplicated identically between pickRecipe and
     * pickRecipeForNutrition, which meant the future-date-pollution fix documented on pickRecipe
     * had to be applied to both call sites by hand. See that doc comment for why distance is
     * absolute day-count, not raw chronological order.
     */
    private List<RecipeResponse> rotationEligibleCandidates(List<RecipeResponse> pool, Map<UUID, LocalDate> lastUsed,
                                                              LocalDate date, int avoidRepeatDays) {
        List<RecipeResponse> eligible = pool.stream()
                .filter(c -> {
                    LocalDate last = lastUsed.get(c.id());
                    return last == null || Math.abs(ChronoUnit.DAYS.between(last, date)) >= avoidRepeatDays;
                })
                .toList();
        return eligible.isEmpty() ? pool : eligible;
    }

    /**
     * First pool entry whose last use (in either direction -- a recipe already booked on a *later*
     * date within the same multi-day generation counts as "recently used" too, not just earlier
     * dates) is outside the rotation window; falls back to the entry used least recently by either
     * measure. Distance is absolute day-count from `date`, not raw chronological order: a recipe
     * booked several days in the future is just as "close" to `date` as one booked that many days in
     * the past, and treating a future date as chronologically "later than everything" broke both the
     * eligibility check (a negative day-count from `ChronoUnit.DAYS.between` never cleared the
     * avoidRepeatDays threshold) and this tie-break (a future lastUsed sorted as more recent than any
     * past one, so a recipe never touched anywhere in the window could keep winning "least recently
     * used" indefinitely -- including within the very meal types just assigned earlier in this same
     * call, since its lastUsed only just got bumped to `date` itself, which is still chronologically
     * "before" every other candidate's future-dated booking).
     */
    private RecipeResponse pickRecipe(List<RecipeResponse> pool, Map<UUID, LocalDate> lastUsed, LocalDate date, int avoidRepeatDays) {
        List<RecipeResponse> candidates = rotationEligibleCandidates(pool, lastUsed, date, avoidRepeatDays);

        // Among candidates, prefer the one used furthest from `date` (never-used sorts first) so the
        // schedule actually rotates instead of always re-picking the first pool entry once its
        // cooldown clears. Ties (e.g. multiple never-used recipes) keep pool order -- which reflects
        // stock priority when that filter is active -- by scanning in order and requiring a strict
        // improvement to replace the current best.
        RecipeResponse best = null;
        long bestDistance = -1;
        for (RecipeResponse candidate : candidates) {
            LocalDate last = lastUsed.get(candidate.id());
            long distance = last == null ? Long.MAX_VALUE : Math.abs(ChronoUnit.DAYS.between(last, date));
            if (best == null || distance > bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Transactional
    public MealPlanResponse confirmCooked(UUID mealPlanId, UUID requesterUserId) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new NotFoundException("Meal plan not found"));
        householdService.requireMembership(mealPlan.getHouseholdId(), requesterUserId);
        if (mealPlan.isCooked()) {
            throw new ConflictException("This meal has already been marked cooked");
        }

        Recipe recipe = recipeRepository.findById(mealPlan.getRecipeId())
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        List<MealPlanPortion> portions = mealPlanPortionRepository.findByMealPlanId(mealPlanId);
        BigDecimal totalPortions = portions.stream().map(MealPlanPortion::getPortionMultiplier)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (recipe.getServings() != null && recipe.getServings() > 0 && totalPortions.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal scaleFactor = totalPortions.divide(BigDecimal.valueOf(recipe.getServings()), 6, RoundingMode.HALF_UP);
            for (RecipeIngredient ri : recipeIngredientRepository.findByRecipeId(recipe.getId())) {
                BigDecimal needed = ri.getQuantity().multiply(scaleFactor).setScale(3, RoundingMode.HALF_UP);
                deductFromPantry(mealPlan.getHouseholdId(), requesterUserId, ri.getIngredientId(), ri.getUnit(), needed);
            }
        }

        mealPlan.setCooked(true);
        mealPlan = mealPlanRepository.save(mealPlan);

        Map<UUID, User> usersById = RepositoryUtils.findAllByIdAsMap(userRepository,
                portions.stream().map(MealPlanPortion::getUserId).distinct().toList(), User::getId);
        MealPlanResponse response = toResponse(mealPlan, recipe, portions, usersById);
        broadcast(mealPlan.getHouseholdId(), MealPlanEvent.Type.MEAL_PLAN_UPDATED, response);
        return response;
    }

    /** Best-effort: deducts from whatever matching pantry stock exists (same ingredient+unit, no conversion);
     * leftover unmet demand is silently ignored since not every ingredient is necessarily pantry-tracked. */
    private void deductFromPantry(UUID householdId, UUID requesterUserId, UUID ingredientId, String unit, BigDecimal needed) {
        BigDecimal remaining = needed;
        List<PantryItem> items = pantryItemRepository.findByHouseholdId(householdId).stream()
                .filter(p -> p.getIngredientId().equals(ingredientId) && p.getUnit().equalsIgnoreCase(unit))
                .toList();
        for (PantryItem item : items) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal deduct = item.getQuantity().min(remaining);
            BigDecimal newQuantity = item.getQuantity().subtract(deduct);
            pantryService.update(item.getId(), requesterUserId,
                    new UpdatePantryItemRequest(null, newQuantity, null, null, null, null, null, null, null, null));
            remaining = remaining.subtract(deduct);
        }
    }

    @Transactional
    public void deleteMealPlan(UUID mealPlanId, UUID requesterUserId) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new NotFoundException("Meal plan not found"));
        householdService.requireMembership(mealPlan.getHouseholdId(), requesterUserId);

        Recipe recipe = recipeRepository.findById(mealPlan.getRecipeId()).orElse(null);
        List<MealPlanPortion> portions = mealPlanPortionRepository.findByMealPlanId(mealPlanId);
        Map<UUID, User> usersById = RepositoryUtils.findAllByIdAsMap(userRepository,
                portions.stream().map(MealPlanPortion::getUserId).distinct().toList(), User::getId);
        MealPlanResponse response = toResponse(mealPlan, recipe, portions, usersById);

        mealPlanPortionRepository.deleteByMealPlanId(mealPlanId);
        mealPlanRepository.delete(mealPlan);
        broadcast(mealPlan.getHouseholdId(), MealPlanEvent.Type.MEAL_PLAN_DELETED, response);
    }

    private MealPlanResponse toResponse(MealPlan mp, Recipe recipe, List<MealPlanPortion> portions, Map<UUID, User> usersById) {
        List<PortionResponse> portionResponses = portions.stream()
                .map(p -> new PortionResponse(p.getUserId(),
                        usersById.containsKey(p.getUserId()) ? usersById.get(p.getUserId()).getDisplayName() : null,
                        p.getPortionMultiplier()))
                .toList();
        return MealPlanResponse.from(mp, recipe, portionResponses);
    }

    private void broadcast(UUID householdId, MealPlanEvent.Type type, MealPlanResponse mealPlan) {
        socketRegistry.broadcastEvent(householdId, new MealPlanEvent(type, mealPlan));
    }
}
