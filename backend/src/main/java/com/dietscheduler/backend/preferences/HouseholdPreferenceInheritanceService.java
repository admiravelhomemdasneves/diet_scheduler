package com.dietscheduler.backend.preferences;

import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.household.Household;
import com.dietscheduler.backend.household.HouseholdMember;
import com.dietscheduler.backend.household.HouseholdMemberRepository;
import com.dietscheduler.backend.household.HouseholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Split out of {@link PreferenceService} so that {@code ensureHouseholdPreferencesInherited} is
 * always invoked through Spring's transactional proxy. {@link PreferenceService} used to call it
 * on itself ({@code this.ensureHouseholdPreferencesInherited(...)}), which bypasses the proxy
 * (Spring's classic self-invocation gap) -- @Transactional silently did nothing, and the
 * pessimistic row lock below requires an active transaction to take effect at all.
 */
@Service
public class HouseholdPreferenceInheritanceService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final UserTasteRepository userTasteRepository;
    private final HouseholdAllergyRepository householdAllergyRepository;
    private final HouseholdTasteRepository householdTasteRepository;

    public HouseholdPreferenceInheritanceService(HouseholdRepository householdRepository,
                                                   HouseholdMemberRepository householdMemberRepository,
                                                   UserAllergyRepository userAllergyRepository,
                                                   UserTasteRepository userTasteRepository,
                                                   HouseholdAllergyRepository householdAllergyRepository,
                                                   HouseholdTasteRepository householdTasteRepository) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.userAllergyRepository = userAllergyRepository;
        this.userTasteRepository = userTasteRepository;
        this.householdAllergyRepository = householdAllergyRepository;
        this.householdTasteRepository = householdTasteRepository;
    }

    /**
     * One-time seed of a household's allergy/taste lists from the union of its current members'
     * personal preferences, the first time either is read. After this runs once (tracked via
     * {@link Household#isPreferencesInherited()}), the household's lists are fully independent --
     * a member removing an inherited entry (or a new member joining later with different
     * preferences) never re-triggers this, matching "inherited once, then editable per household"
     * rather than a live sync. Also called from {@code RecipeService} before computing exclusion
     * filters, so recipe filtering reflects inherited preferences even for a household whose
     * members never open the household-preferences screen.
     */
    @Transactional
    public void ensureHouseholdPreferencesInherited(UUID householdId) {
        // Pessimistic lock: allergies and tastes are loaded concurrently by the client, and both
        // trigger this method. Without serializing on the household row, two concurrent callers
        // can both observe preferencesInherited == false and race to insert the same rows, so the
        // loser hits a unique-constraint violation. Locking makes the second caller block until
        // the first commits, then see the now-true flag and return immediately.
        Household household = householdRepository.findByIdForUpdate(householdId)
                .orElseThrow(() -> new NotFoundException("Household not found"));
        if (household.isPreferencesInherited()) {
            return;
        }

        List<UUID> memberUserIds = householdMemberRepository.findByHouseholdId(householdId).stream()
                .map(HouseholdMember::getUserId).toList();

        if (!memberUserIds.isEmpty()) {
            userAllergyRepository.findByUserIdIn(memberUserIds).stream()
                    .map(UserAllergy::getAllergyId).distinct()
                    .forEach(allergyId -> householdAllergyRepository.findByHouseholdIdAndAllergyId(householdId, allergyId)
                            .orElseGet(() -> householdAllergyRepository.save(
                                    HouseholdAllergy.builder().householdId(householdId).allergyId(allergyId).build())));

            // When members disagree on a taste, the most restrictive tier wins (enum declaration
            // order FAVORITE < LIKED < DISLIKED < FORBIDDEN doubles as a restrictiveness ranking) --
            // safer default for a shared exclusion filter than picking one member's view arbitrarily.
            Map<UUID, TastePreference> strictestByTasteId = new HashMap<>();
            userTasteRepository.findByUserIdIn(memberUserIds)
                    .forEach(ut -> strictestByTasteId.merge(ut.getTasteId(), ut.getPreference(),
                            (a, b) -> a.compareTo(b) >= 0 ? a : b));
            strictestByTasteId.forEach((tasteId, preference) ->
                    householdTasteRepository.findByHouseholdIdAndTasteId(householdId, tasteId)
                            .orElseGet(() -> householdTasteRepository.save(
                                    HouseholdTaste.builder().householdId(householdId).tasteId(tasteId).preference(preference).build())));
        }

        household.setPreferencesInherited(true);
        householdRepository.save(household);
    }
}
