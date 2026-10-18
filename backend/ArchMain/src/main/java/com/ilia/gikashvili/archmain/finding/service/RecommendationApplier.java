package com.ilia.gikashvili.archmain.finding.service;

import com.ilia.gikashvili.archmain.finding.dto.RecommendationPreviewResponse;
import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates an architectural preview patch for a recommendation.
 * ArchMain returns preview patches and never silently mutates diagrams.
 */
@Component
public class RecommendationApplier {

    public RecommendationPreviewResponse generatePreviewPatch(ReviewFinding finding) {
        String category = finding.getCategory() != null ? finding.getCategory().toUpperCase() : "";
        Map<String, Object> patch = new HashMap<>();

        if ("SCALABILITY".equals(category) || finding.getTitle().toLowerCase().contains("cache")) {
            patch.put("action", "ADD_NODE");
            patch.put("proposedNodeType", "cache");
            patch.put("proposedLabel", "Redis Cache");
            patch.put("suggestedPosition", Map.of("x", 250.0, "y", 100.0));
            patch.put("targetAffectedNode", finding.getAffectedElementId());
            return new RecommendationPreviewResponse(
                    finding.getId(),
                    finding.getRecommendation(),
                    "ADD_CACHE_LAYER",
                    "Introduce a distributed caching layer (Redis) in front of datastores.",
                    patch
            );
        } else if ("PERFORMANCE_AND_RELIABILITY".equals(category) || finding.getTitle().toLowerCase().contains("bottleneck")) {
            patch.put("action", "ADD_NODE");
            patch.put("proposedNodeType", "database-replica");
            patch.put("proposedLabel", "Read Replica");
            patch.put("suggestedPosition", Map.of("x", 350.0, "y", 200.0));
            patch.put("targetAffectedNode", finding.getAffectedElementId());
            return new RecommendationPreviewResponse(
                    finding.getId(),
                    finding.getRecommendation(),
                    "ADD_READ_REPLICA",
                    "Add read replica to distribute query load away from primary database.",
                    patch
            );
        } else {
            patch.put("action", "ANNOTATE_NODE");
            patch.put("targetNodeId", finding.getAffectedElementId());
            patch.put("annotation", finding.getRecommendation());
            return new RecommendationPreviewResponse(
                    finding.getId(),
                    finding.getRecommendation(),
                    "GENERIC_RECOMMENDATION",
                    "Apply architecture annotation to address finding.",
                    patch
            );
        }
    }
}
