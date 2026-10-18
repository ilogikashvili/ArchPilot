package com.ilia.gikashvili.archmain.project.entity;

import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * A project that contains an architecture document.
 */
@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private boolean archived = false;

    // --- Getters & Setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
