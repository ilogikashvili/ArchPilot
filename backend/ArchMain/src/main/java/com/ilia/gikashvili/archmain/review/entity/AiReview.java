package com.ilia.gikashvili.archmain.review.entity;

import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
import com.ilia.gikashvili.archmain.review.state.ReviewStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An AI/rule-based review of a project's architecture.
 */
@Entity
@Table(name = "ai_reviews")
public class AiReview extends BaseEntity {

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.QUEUED;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    @Column(length = 2000)
    private String summary;

    @Column
    private int findingCount;

    // --- Getters & Setters ---

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getFindingCount() {
        return findingCount;
    }

    public void setFindingCount(int findingCount) {
        this.findingCount = findingCount;
    }
}
