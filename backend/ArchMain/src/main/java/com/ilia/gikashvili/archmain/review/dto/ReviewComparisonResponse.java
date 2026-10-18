package com.ilia.gikashvili.archmain.review.dto;

import java.util.List;
import java.util.UUID;

public record ReviewComparisonResponse(
        UUID baseReviewId,
        UUID targetReviewId,
        int newFindingsCount,
        int resolvedFindingsCount,
        int unchangedFindingsCount,
        List<FindingResponse> newFindings,
        List<FindingResponse> resolvedFindings,
        List<FindingResponse> unchangedFindings
) {
}
