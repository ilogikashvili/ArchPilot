package com.ilia.gikashvili.archmain.review.entity;

import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * A single finding from a review.
 */
@Entity
@Table(name = "review_findings")
public class ReviewFinding extends BaseEntity {

    @Column(nullable = false)
    private UUID reviewId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String severity; // CRITICAL, WARNING, INFO, SUGGESTION

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 4000)
    private String recommendation;

    @Column
    private String affectedElementId;

    // --- Getters & Setters ---

    public UUID getReviewId() {
        return reviewId;
    }

    public void setReviewId(UUID reviewId) {
        this.reviewId = reviewId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getAffectedElementId() {
        return affectedElementId;
    }

    public void setAffectedElementId(String affectedElementId) {
        this.affectedElementId = affectedElementId;
    }
}
