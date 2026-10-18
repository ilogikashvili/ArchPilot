package com.ilia.gikashvili.archmain.architecture.entity;

import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity wrapping the architecture document stored as JSONB.
 */
@Entity
@Table(name = "architecture_documents")
public class ArchitectureDocumentEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID projectId;

    /**
     * The complete architecture document serialized as JSON.
     * Stored as JSONB in PostgreSQL, TEXT in H2.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String documentJson;

    /**
     * Optimistic concurrency: callers must send the current revision when saving.
     */
    @Version
    private long revision;

    private UUID updatedBy;
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID value) { updatedBy = value; }

    // --- Getters & Setters ---

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getDocumentJson() {
        return documentJson;
    }

    public void setDocumentJson(String documentJson) {
        this.documentJson = documentJson;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }
}
