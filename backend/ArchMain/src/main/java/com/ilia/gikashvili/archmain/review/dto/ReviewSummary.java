package com.ilia.gikashvili.archmain.review.dto;

import com.ilia.gikashvili.archmain.review.state.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight summary of an architecture review for list responses.
 */
public record ReviewSummary(
        UUID id,
        UUID projectId,
        ReviewStatus status,
        int findingCount,
        Instant createdAt
) {
}
