package com.ilia.gikashvili.archmain.finding.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Preview patch generated for a recommendation.
 * ArchMain returns preview proposals rather than silently mutating the diagram.
 */
public record RecommendationPreviewResponse(
        UUID findingId,
        String recommendation,
        String patchType,
        String patchDescription,
        Map<String, Object> previewPatch
) {
}
