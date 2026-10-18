package com.ilia.gikashvili.archmain.security;

import java.util.UUID;

/**
 * Lightweight principal object stored in the SecurityContext.
 */
public record AuthenticatedUser(
        UUID id,
        String email
) {
}
