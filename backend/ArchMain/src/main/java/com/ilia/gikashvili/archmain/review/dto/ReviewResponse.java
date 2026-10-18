package com.ilia.gikashvili.archmain.review.dto;

import com.ilia.gikashvili.archmain.review.state.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Full details of an AI/automated review.
 */
public record ReviewResponse(
        UUID id,
        UUID projectId,
        UUID requestedBy,
        ReviewStatus status,
        Instant startedAt,
        Instant completedAt,
        String summary,
        int findingCount,
        Instant createdAt
) {
}
