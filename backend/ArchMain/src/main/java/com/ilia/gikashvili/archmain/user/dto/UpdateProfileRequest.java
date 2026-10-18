package com.ilia.gikashvili.archmain.user.dto;

import jakarta.validation.constraints.Size;

/**
 * Request to update the current user's profile.
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
        String displayName,

        String avatarUrl
) {
}
