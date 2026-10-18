package com.ilia.gikashvili.archmain.review.engine.rules;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;

import java.util.List;
import java.util.UUID;

/**
 * Strategy interface for an individual architectural review check.
 */
public interface ReviewRule {

    List<ReviewFinding> check(UUID reviewId, UUID projectId, ArchitectureDocument document);
}
