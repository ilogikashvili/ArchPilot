package com.ilia.gikashvili.archmain.auth.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for GET /auth/me — the current user's identity.
 */
public record CurrentUserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        boolean emailVerified,
        Instant createdAt
) {
}
