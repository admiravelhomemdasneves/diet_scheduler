package com.dietscheduler.backend.preferences;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TasteRepository extends JpaRepository<Taste, UUID> {
    Optional<Taste> findByTypeAndNameIgnoreCase(TasteType type, String name);
}
