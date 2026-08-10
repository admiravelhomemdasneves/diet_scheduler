package com.dietscheduler.backend.household;

import java.util.UUID;

/** Broadcast on the household's own WS channel right before its data is torn down, so any other
 * member still connected (household deleted by its owner while they're actively using the app)
 * reacts immediately instead of hitting confusing 404s on their next request. */
public record HouseholdDeletedEvent(Type type, UUID householdId) {
    public enum Type {
        HOUSEHOLD_DELETED
    }
}
