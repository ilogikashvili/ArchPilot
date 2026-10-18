package com.ilia.gikashvili.archmain.finding.dto;

import java.time.Instant;
import java.util.UUID;

public record DecisionResponse(
        UUID id,
        UUID projectId,
        UUID findingId,
        UUID decidedBy,
        String decision,
        String reason,
        Instant decidedAt,
        Instant createdAt
) {
}
