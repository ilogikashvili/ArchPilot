package com.ilia.gikashvili.archmain.review.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CompareReviewsRequest(
        @NotNull(message = "Base review ID is required")
        UUID baseReviewId,

        @NotNull(message = "Target review ID is required")
        UUID targetReviewId
) {
}
