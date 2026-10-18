package com.ilia.gikashvili.archmain.architecture.dto;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import jakarta.validation.constraints.NotNull;

public record SaveArchitectureRequest(
        @NotNull(message = "Document is required")
        ArchitectureDocument document,

        @NotNull(message = "Revision is required for optimistic concurrency")
        Long expectedRevision
) {
}
