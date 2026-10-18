package com.ilia.gikashvili.archmain.review.engine;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.review.engine.rules.ReviewRule;
import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MVP rule-based implementation of ArchitectureReviewEngine.
 * Uses Chain of Responsibility / rule list to execute all registered review rules.
 */
@Component
public class RuleBasedReviewEngine implements ArchitectureReviewEngine {

    private final List<ReviewRule> rules;

    public RuleBasedReviewEngine(List<ReviewRule> rules) {
        this.rules = rules;
    }

    @Override
    public List<ReviewFinding> review(UUID reviewId, UUID projectId, ArchitectureDocument document) {
        List<ReviewFinding> allFindings = new ArrayList<>();
        for (ReviewRule rule : rules) {
            allFindings.addAll(rule.check(reviewId, projectId, document));
        }
        return allFindings;
    }
}
