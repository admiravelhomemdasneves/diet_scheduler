package com.dietscheduler.backend.household;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
    Optional<Household> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);

    /** Row-locks the household for the duration of the caller's transaction, so concurrent
     * callers of {@code ensureHouseholdPreferencesInherited} serialize instead of racing to
     * insert the same inherited rows. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Household h where h.id = :id")
    Optional<Household> findByIdForUpdate(@Param("id") UUID id);
}
