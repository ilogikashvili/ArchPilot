package com.ilia.gikashvili.archmain.finding.dto;

import java.time.Instant;
import java.util.UUID;

public record DecisionHistoryResponse(
        UUID decisionId,
        UUID findingId,
        UUID decidedBy,
        String decision,
        String reason,
        Instant decidedAt
) {
}
