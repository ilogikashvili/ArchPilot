package com.ilia.gikashvili.archmain.user.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * User profile response DTO.
 */
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        boolean emailVerified,
        Instant createdAt
) {
}
