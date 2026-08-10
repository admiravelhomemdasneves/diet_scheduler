package com.dietscheduler.backend.household;

import com.dietscheduler.backend.common.ConflictException;
import com.dietscheduler.backend.common.ForbiddenException;
import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.common.RepositoryUtils;
import com.dietscheduler.backend.household.dto.HouseholdResponse;
import com.dietscheduler.backend.household.dto.MemberResponse;
import com.dietscheduler.backend.mealplan.MealPlan;
import com.dietscheduler.backend.mealplan.MealPlanPortionRepository;
import com.dietscheduler.backend.mealplan.MealPlanRepository;
import com.dietscheduler.backend.pantry.IngredientRepository;
import com.dietscheduler.backend.pantry.PantryItemRepository;
import com.dietscheduler.backend.preferences.HouseholdAllergyRepository;
import com.dietscheduler.backend.preferences.HouseholdTasteRepository;
import com.dietscheduler.backend.shoppinglist.IgnoredMissingIngredientRepository;
import com.dietscheduler.backend.shoppinglist.ShoppingListItemRepository;
import com.dietscheduler.backend.user.User;
import com.dietscheduler.backend.user.UserRepository;
import com.dietscheduler.backend.ws.HouseholdSocketRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HouseholdService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final int INVITE_CODE_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UserRepository userRepository;
    private final HouseholdAllergyRepository householdAllergyRepository;
    private final HouseholdTasteRepository householdTasteRepository;
    private final PantryItemRepository pantryItemRepository;
    private final IngredientRepository ingredientRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanPortionRepository mealPlanPortionRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final IgnoredMissingIngredientRepository ignoredMissingIngredientRepository;
    private final HouseholdSocketRegistry socketRegistry;

    public HouseholdService(HouseholdRepository householdRepository, HouseholdMemberRepository householdMemberRepository,
                             UserRepository userRepository, HouseholdAllergyRepository householdAllergyRepository,
                             HouseholdTasteRepository householdTasteRepository, PantryItemRepository pantryItemRepository,
                             IngredientRepository ingredientRepository, MealPlanRepository mealPlanRepository,
                             MealPlanPortionRepository mealPlanPortionRepository, ShoppingListItemRepository shoppingListItemRepository,
                             IgnoredMissingIngredientRepository ignoredMissingIngredientRepository,
                             HouseholdSocketRegistry socketRegistry) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.userRepository = userRepository;
        this.householdAllergyRepository = householdAllergyRepository;
        this.householdTasteRepository = householdTasteRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.ingredientRepository = ingredientRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanPortionRepository = mealPlanPortionRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.ignoredMissingIngredientRepository = ignoredMissingIngredientRepository;
        this.socketRegistry = socketRegistry;
    }

    @Transactional
    public Household createHousehold(UUID creatorUserId, String name) {
        Household household = Household.builder()
                .name(name)
                .createdBy(creatorUserId)
                .inviteCode(generateUniqueInviteCode())
                .build();
        household = householdRepository.save(household);

        HouseholdMember owner = HouseholdMember.builder()
                .householdId(household.getId())
                .userId(creatorUserId)
                .role(HouseholdRole.OWNER)
                .build();
        householdMemberRepository.save(owner);

        return household;
    }

    /** Checks membership before existence, and reports both failure modes identically ("not a
     * member") -- checking existence first would let a caller distinguish "this household doesn't
     * exist" from "it exists but you're not in it" purely from the HTTP status, which is an
     * unnecessary existence oracle for households the caller has no business knowing about. */
    public Household getHouseholdForMember(UUID householdId, UUID requesterUserId) {
        HouseholdMember membership = householdMemberRepository.findByHouseholdIdAndUserId(householdId, requesterUserId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this household"));
        // The household is guaranteed to exist here: this membership row can only exist if it did
        // at creation time, and nothing in this codebase deletes a household without also deleting
        // its household_member rows in the same transaction (see HouseholdService.deleteHouseholdData).
        return householdRepository.findById(membership.getHouseholdId())
                .orElseThrow(() -> new NotFoundException("Household not found"));
    }

    // Package-private: only used internally (see leaveHousehold below); no caller outside this
    // class or package needs the raw membership list today.
    List<HouseholdMember> getMembers(UUID householdId) {
        return householdMemberRepository.findByHouseholdId(householdId);
    }

    /** All households the given user currently belongs to. */
    public List<Household> listForUser(UUID userId) {
        List<UUID> householdIds = householdMemberRepository.findByUserId(userId).stream()
                .map(HouseholdMember::getHouseholdId)
                .toList();
        return householdRepository.findAllById(householdIds);
    }

    public HouseholdResponse toResponse(Household household) {
        List<HouseholdMember> members = getMembers(household.getId());
        Map<UUID, User> usersById = RepositoryUtils.findAllByIdAsMap(userRepository,
                members.stream().map(HouseholdMember::getUserId).toList(), User::getId);

        List<MemberResponse> memberResponses = members.stream()
                .map(m -> {
                    User u = usersById.get(m.getUserId());
                    return new MemberResponse(m.getUserId(), u != null ? u.getDisplayName() : null,
                            u != null ? u.getEmail() : null, m.getRole());
                })
                .toList();

        return HouseholdResponse.from(household, memberResponses);
    }

    public String getInviteCodeForOwner(UUID householdId, UUID requesterUserId) {
        HouseholdMember membership = householdMemberRepository.findByHouseholdIdAndUserId(householdId, requesterUserId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this household"));
        if (membership.getRole() != HouseholdRole.OWNER) {
            throw new ForbiddenException("Only the household owner can view/regenerate the invite code");
        }
        Household household = householdRepository.findById(membership.getHouseholdId())
                .orElseThrow(() -> new NotFoundException("Household not found"));
        return household.getInviteCode();
    }

    /** Owner-only. Invalidates the current invite code and issues a new one -- e.g. after
     * accidentally sharing it somewhere public, or just as routine hygiene, since the code itself
     * never expires or rotates on its own otherwise. */
    @Transactional
    public String regenerateInviteCode(UUID householdId, UUID requesterUserId) {
        HouseholdMember membership = householdMemberRepository.findByHouseholdIdAndUserId(householdId, requesterUserId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this household"));
        if (membership.getRole() != HouseholdRole.OWNER) {
            throw new ForbiddenException("Only the household owner can regenerate the invite code");
        }
        Household household = householdRepository.findById(membership.getHouseholdId())
                .orElseThrow(() -> new NotFoundException("Household not found"));
        household.setInviteCode(generateUniqueInviteCode());
        householdRepository.save(household);
        return household.getInviteCode();
    }

    @Transactional
    public HouseholdMember joinByInviteCode(String inviteCode, UUID userId) {
        Household household = householdRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new NotFoundException("Invalid invite code"));

        return householdMemberRepository.findByHouseholdIdAndUserId(household.getId(), userId)
                .orElseGet(() -> householdMemberRepository.save(HouseholdMember.builder()
                        .householdId(household.getId())
                        .userId(userId)
                        .role(HouseholdRole.MEMBER)
                        .build()));
    }

    public void requireMembership(UUID householdId, UUID userId) {
        if (!householdMemberRepository.existsByHouseholdIdAndUserId(householdId, userId)) {
            throw new ForbiddenException("Not a member of this household");
        }
    }

    /**
     * Removes the requester's own membership. If that was the last member, the household (and all
     * its data) is torn down immediately -- an abandoned household with zero members has no owner
     * left to manage or delete it later, so leaving the empty one behind would just be permanent
     * clutter with no way for anyone to clean it up.
     *
     * The one case this refuses: the sole OWNER leaving while other MEMBERs remain. There's no
     * ownership-transfer flow yet, so allowing that would strand the survivors -- nobody left who
     * can view/regenerate the invite code (owner-only) or delete the household. Deleting it
     * outright, or waiting until you're the last one left, are the two ways out for now.
     */
    @Transactional
    public void leaveHousehold(UUID householdId, UUID userId) {
        HouseholdMember membership = householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId)
                .orElseThrow(() -> new NotFoundException("Not a member of this household"));

        boolean otherMembersRemain = householdMemberRepository.findByHouseholdId(householdId).stream()
                .anyMatch(m -> !m.getUserId().equals(userId));
        if (membership.getRole() == HouseholdRole.OWNER && otherMembersRemain) {
            throw new ConflictException("You're the only owner and other members remain. "
                    + "Delete the household instead, or leave once you're the last member.");
        }

        householdMemberRepository.delete(membership);
        if (otherMembersRemain) {
            socketRegistry.closeSessionsForUser(householdId, userId);
        } else {
            deleteHouseholdData(householdId);
        }
    }

    /** Owner-only: deletes the household and all its data outright, regardless of member count. */
    @Transactional
    public void deleteHousehold(UUID householdId, UUID requesterUserId) {
        HouseholdMember membership = householdMemberRepository.findByHouseholdIdAndUserId(householdId, requesterUserId)
                .orElseThrow(() -> new NotFoundException("Not a member of this household"));
        if (membership.getRole() != HouseholdRole.OWNER) {
            throw new ForbiddenException("Only the household owner can delete the household");
        }
        broadcastDeleted(householdId);
        deleteHouseholdData(householdId);
    }

    /** Deletes every row scoped to this household, across every table that carries a household_id,
     * then the household row itself. Recipes are deliberately excluded -- they're globally visible
     * (see {@code Recipe}'s own doc comment), never owned by a household. */
    private void deleteHouseholdData(UUID householdId) {
        List<UUID> mealPlanIds = mealPlanRepository.findByHouseholdId(householdId).stream().map(MealPlan::getId).toList();
        mealPlanIds.forEach(mealPlanPortionRepository::deleteByMealPlanId);
        mealPlanRepository.deleteByHouseholdId(householdId);
        shoppingListItemRepository.deleteByHouseholdId(householdId);
        ignoredMissingIngredientRepository.deleteByHouseholdId(householdId);
        pantryItemRepository.deleteByHouseholdId(householdId);
        ingredientRepository.deleteByHouseholdId(householdId);
        householdAllergyRepository.deleteByHouseholdId(householdId);
        householdTasteRepository.deleteByHouseholdId(householdId);
        householdMemberRepository.deleteByHouseholdId(householdId);
        householdRepository.deleteById(householdId);
        // Any member still actively connected (owner-initiated delete while others are using the
        // app live, or the empty-household auto-cascade from leaveHousehold) gets disconnected --
        // otherwise they'd keep receiving/being able to send events for a household that's gone.
        socketRegistry.closeAll(householdId);
    }

    private void broadcastDeleted(UUID householdId) {
        try {
            socketRegistry.broadcastEvent(householdId,
                    new HouseholdDeletedEvent(HouseholdDeletedEvent.Type.HOUSEHOLD_DELETED, householdId));
        } catch (Exception e) {
            // Best-effort notification; the household is deleted either way.
        }
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                sb.append(INVITE_CODE_ALPHABET.charAt(random.nextInt(INVITE_CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (householdRepository.existsByInviteCode(code));
        return code;
    }
}
