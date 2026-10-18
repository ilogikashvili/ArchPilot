package com.ilia.gikashvili.archmain.project.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
}
