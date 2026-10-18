package com.ilia.gikashvili.archmain.review.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Details of an individual review finding.
 */
public record FindingResponse(
        UUID id,
        UUID reviewId,
        UUID projectId,
        String severity,
        String category,
        String title,
        String description,
        String recommendation,
        String affectedElementId,
        Instant createdAt
) {
}
