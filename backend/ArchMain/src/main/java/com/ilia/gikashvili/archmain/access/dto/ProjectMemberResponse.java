package com.ilia.gikashvili.archmain.access.dto;

import com.ilia.gikashvili.archmain.access.policy.ProjectRole;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        ProjectRole role,
        Instant createdAt
) {
}
