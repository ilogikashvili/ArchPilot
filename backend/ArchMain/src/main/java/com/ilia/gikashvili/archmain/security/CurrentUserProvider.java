package com.ilia.gikashvili.archmain.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads the authenticated user from the Spring SecurityContext.
 */
@Component
public class CurrentUserProvider {

    /**
     * Get the currently authenticated user, if any.
     */
    public Optional<AuthenticatedUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Get the currently authenticated user or throw.
     */
    public AuthenticatedUser requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }

    /**
     * Get the current user's ID.
     */
    public UUID requireCurrentUserId() {
        return requireCurrentUser().id();
    }
}
