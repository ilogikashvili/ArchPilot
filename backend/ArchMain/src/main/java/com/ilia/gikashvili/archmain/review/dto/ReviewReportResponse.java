package com.ilia.gikashvili.archmain.review.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReviewReportResponse(
        UUID reviewId,
        UUID projectId,
        Instant generatedAt,
        String status,
        int totalFindings,
        Map<String, Long> findingsBySeverity,
        Map<String, Long> findingsByCategory,
        List<FindingResponse> criticalFindings,
        List<String> keyRecommendations
) {
}
