package com.dietscheduler.backend.preferences;

import com.dietscheduler.backend.common.NotFoundException;
import com.dietscheduler.backend.common.RepositoryUtils;
import com.dietscheduler.backend.household.HouseholdService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PreferenceService {

    private final AllergyRepository allergyRepository;
    private final TasteRepository tasteRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final UserTasteRepository userTasteRepository;
    private final HouseholdTasteRepository householdTasteRepository;
    private final HouseholdAllergyRepository householdAllergyRepository;
    private final HouseholdService householdService;
    private final HouseholdPreferenceInheritanceService inheritanceService;

    public PreferenceService(AllergyRepository allergyRepository, TasteRepository tasteRepository,
                              UserAllergyRepository userAllergyRepository, UserTasteRepository userTasteRepository,
                              HouseholdTasteRepository householdTasteRepository, HouseholdAllergyRepository householdAllergyRepository,
                              HouseholdService householdService, HouseholdPreferenceInheritanceService inheritanceService) {
        this.allergyRepository = allergyRepository;
        this.tasteRepository = tasteRepository;
        this.userAllergyRepository = userAllergyRepository;
        this.userTasteRepository = userTasteRepository;
        this.householdTasteRepository = householdTasteRepository;
        this.householdAllergyRepository = householdAllergyRepository;
        this.householdService = householdService;
        this.inheritanceService = inheritanceService;
    }

    public List<Allergy> listAllergies() {
        return allergyRepository.findAll();
    }

    public List<Taste> listTastes() {
        return tasteRepository.findAll();
    }

    public List<Allergy> getUserAllergies(UUID userId) {
        List<UUID> allergyIds = userAllergyRepository.findByUserId(userId).stream()
                .map(UserAllergy::getAllergyId).toList();
        return allergyRepository.findAllById(allergyIds);
    }

    @Transactional
    public void addUserAllergy(UUID userId, UUID allergyId) {
        if (!allergyRepository.existsById(allergyId)) {
            throw new NotFoundException("Allergy not found");
        }
        userAllergyRepository.findByUserIdAndAllergyId(userId, allergyId)
                .orElseGet(() -> userAllergyRepository.save(
                        UserAllergy.builder().userId(userId).allergyId(allergyId).build()));
    }

    @Transactional
    public void removeUserAllergy(UUID userId, UUID allergyId) {
        userAllergyRepository.findByUserIdAndAllergyId(userId, allergyId)
                .ifPresent(userAllergyRepository::delete);
    }

    public List<Map.Entry<Taste, TastePreference>> getUserTastes(UUID userId) {
        List<UserTaste> entries = userTasteRepository.findByUserId(userId);
        Map<UUID, Taste> tastesById = RepositoryUtils.findAllByIdAsMap(tasteRepository,
                entries.stream().map(UserTaste::getTasteId).toList(), Taste::getId);
        return entries.stream()
                .filter(e -> tastesById.containsKey(e.getTasteId()))
                .map(e -> Map.entry(tastesById.get(e.getTasteId()), e.getPreference()))
                .toList();
    }

    @Transactional
    public void setUserTaste(UUID userId, UUID tasteId, TastePreference preference) {
        Taste taste = tasteRepository.findById(tasteId)
                .orElseThrow(() -> new NotFoundException("Taste not found"));
        UserTaste entry = userTasteRepository.findByUserIdAndTasteId(userId, taste.getId())
                .orElseGet(() -> UserTaste.builder().userId(userId).tasteId(taste.getId()).build());
        entry.setPreference(preference);
        userTasteRepository.save(entry);
    }

    @Transactional
    public void removeUserTaste(UUID userId, UUID tasteId) {
        userTasteRepository.findByUserIdAndTasteId(userId, tasteId)
                .ifPresent(userTasteRepository::delete);
    }

    public List<Allergy> getHouseholdAllergies(UUID householdId, UUID requesterUserId) {
        householdService.requireMembership(householdId, requesterUserId);
        inheritanceService.ensureHouseholdPreferencesInherited(householdId);
        List<UUID> allergyIds = householdAllergyRepository.findByHouseholdId(householdId).stream()
                .map(HouseholdAllergy::getAllergyId).toList();
        return allergyRepository.findAllById(allergyIds);
    }

    @Transactional
    public void addHouseholdAllergy(UUID householdId, UUID requesterUserId, UUID allergyId) {
        householdService.requireMembership(householdId, requesterUserId);
        if (!allergyRepository.existsById(allergyId)) {
            throw new NotFoundException("Allergy not found");
        }
        householdAllergyRepository.findByHouseholdIdAndAllergyId(householdId, allergyId)
                .orElseGet(() -> householdAllergyRepository.save(
                        HouseholdAllergy.builder().householdId(householdId).allergyId(allergyId).build()));
    }

    @Transactional
    public void removeHouseholdAllergy(UUID householdId, UUID requesterUserId, UUID allergyId) {
        householdService.requireMembership(householdId, requesterUserId);
        householdAllergyRepository.findByHouseholdIdAndAllergyId(householdId, allergyId)
                .ifPresent(householdAllergyRepository::delete);
    }

    public List<Map.Entry<Taste, TastePreference>> getHouseholdTastes(UUID householdId, UUID requesterUserId) {
        householdService.requireMembership(householdId, requesterUserId);
        inheritanceService.ensureHouseholdPreferencesInherited(householdId);
        List<HouseholdTaste> entries = householdTasteRepository.findByHouseholdId(householdId);
        Map<UUID, Taste> tastesById = RepositoryUtils.findAllByIdAsMap(tasteRepository,
                entries.stream().map(HouseholdTaste::getTasteId).toList(), Taste::getId);
        return entries.stream()
                .filter(e -> tastesById.containsKey(e.getTasteId()))
                .map(e -> Map.entry(tastesById.get(e.getTasteId()), e.getPreference()))
                .toList();
    }

    @Transactional
    public void setHouseholdTaste(UUID householdId, UUID requesterUserId, UUID tasteId, TastePreference preference) {
        householdService.requireMembership(householdId, requesterUserId);
        Taste taste = tasteRepository.findById(tasteId)
                .orElseThrow(() -> new NotFoundException("Taste not found"));
        HouseholdTaste entry = householdTasteRepository.findByHouseholdIdAndTasteId(householdId, taste.getId())
                .orElseGet(() -> HouseholdTaste.builder().householdId(householdId).tasteId(taste.getId()).build());
        entry.setPreference(preference);
        householdTasteRepository.save(entry);
    }

    @Transactional
    public void removeHouseholdTaste(UUID householdId, UUID requesterUserId, UUID tasteId) {
        householdService.requireMembership(householdId, requesterUserId);
        householdTasteRepository.findByHouseholdIdAndTasteId(householdId, tasteId)
                .ifPresent(householdTasteRepository::delete);
    }

    /** @see HouseholdPreferenceInheritanceService#ensureHouseholdPreferencesInherited(UUID) */
    public void ensureHouseholdPreferencesInherited(UUID householdId) {
        inheritanceService.ensureHouseholdPreferencesInherited(householdId);
    }
}
