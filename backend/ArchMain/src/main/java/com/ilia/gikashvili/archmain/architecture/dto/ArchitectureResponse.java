package com.ilia.gikashvili.archmain.architecture.dto;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;

import java.time.Instant;
import java.util.UUID;

public record ArchitectureResponse(
        UUID projectId,
        ArchitectureDocument document,
        long revision,
        Instant updatedAt
) {
}
