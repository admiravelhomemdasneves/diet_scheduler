package com.dietscheduler.backend.user;

import com.dietscheduler.backend.common.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAuthProviderAndProviderSubjectId(AuthProvider authProvider, String providerSubjectId);

    /** {@code findById(id).orElseThrow(...)} was repeated identically in four controllers. The
     * caller is always resolving the *authenticated* user's own id (from the JWT), so a miss here
     * means the user row was deleted out from under a still-valid token, not a client input error. */
    default User findRequiredById(UUID id) {
        return findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
