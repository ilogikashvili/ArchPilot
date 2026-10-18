package com.ilia.gikashvili.archmain.review.mapper;

import com.ilia.gikashvili.archmain.review.dto.FindingResponse;
import com.ilia.gikashvili.archmain.review.dto.ReviewResponse;
import com.ilia.gikashvili.archmain.review.dto.ReviewSummary;
import com.ilia.gikashvili.archmain.review.entity.AiReview;
import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(AiReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getProjectId(),
                review.getRequestedBy(),
                review.getStatus(),
                review.getStartedAt(),
                review.getCompletedAt(),
                review.getSummary(),
                review.getFindingCount(),
                review.getCreatedAt()
        );
    }

    public ReviewSummary toSummary(AiReview review) {
        return new ReviewSummary(
                review.getId(),
                review.getProjectId(),
                review.getStatus(),
                review.getFindingCount(),
                review.getCreatedAt()
        );
    }

    public FindingResponse toFindingResponse(ReviewFinding finding) {
        return new FindingResponse(
                finding.getId(),
                finding.getReviewId(),
                finding.getProjectId(),
                finding.getSeverity(),
                finding.getCategory(),
                finding.getTitle(),
                finding.getDescription(),
                finding.getRecommendation(),
                finding.getAffectedElementId(),
                finding.getCreatedAt()
        );
    }
}
