package com.ilia.gikashvili.archmain.finding.entity;

import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Record of a decision made on a review finding (e.g. ACCEPTED, DISMISSED).
 */
@Entity
@Table(name = "finding_decisions")
public class FindingDecision extends BaseEntity {

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID findingId;

    @Column(nullable = false)
    private UUID decidedBy;

    @Column(nullable = false, length = 50)
    private String decision; // ACCEPTED, DISMISSED, etc.

    @Column(length = 2000)
    private String reason;

    @Column(nullable = false)
    private Instant decidedAt;

    // --- Getters & Setters ---

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getFindingId() {
        return findingId;
    }

    public void setFindingId(UUID findingId) {
        this.findingId = findingId;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
