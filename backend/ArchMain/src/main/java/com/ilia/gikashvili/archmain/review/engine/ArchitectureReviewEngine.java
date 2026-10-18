package com.ilia.gikashvili.archmain.review.engine;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;

import java.util.List;
import java.util.UUID;

/**
 * STRATEGY interface for evaluating an architecture document and producing findings.
 */
public interface ArchitectureReviewEngine {

    List<ReviewFinding> review(UUID reviewId, UUID projectId, ArchitectureDocument document);
}
