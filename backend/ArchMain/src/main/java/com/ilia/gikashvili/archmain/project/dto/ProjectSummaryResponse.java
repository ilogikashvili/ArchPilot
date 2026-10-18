package com.ilia.gikashvili.archmain.project.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary view of a project with metrics (node, edge, member counts).
 */
public record ProjectSummaryResponse(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        boolean archived,
        int nodeCount,
        int edgeCount,
        int memberCount,
        Instant updatedAt
) {
}
