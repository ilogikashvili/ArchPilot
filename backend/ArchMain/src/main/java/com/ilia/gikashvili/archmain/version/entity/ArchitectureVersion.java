package com.ilia.gikashvili.archmain.version.entity;
import jakarta.persistence.*;
import java.util.UUID;
import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
@Entity
@Table(name="version_documents")
public class ArchitectureVersion extends BaseEntity {
public UUID parentVersionId;
public UUID branchId;
public String branchName="main";
@Column(length=2000) public String message;
private UUID projectId;
public UUID getProjectId() { return projectId; }
public void setProjectId(UUID value) { this.projectId=value; }
private String name;
public String getName() { return name; }
public void setName(String value) { this.name=value; }
@Column(columnDefinition="TEXT")
private String description;
public String getDescription() { return description; }
public void setDescription(String value) { this.description=value; }
@Column(columnDefinition="TEXT")
private String documentJson;
public String getDocumentJson() { return documentJson; }
public void setDocumentJson(String value) { this.documentJson=value; }
private UUID createdBy;
public UUID getCreatedBy() { return createdBy; }
public void setCreatedBy(UUID value) { this.createdBy=value; }
private long sourceRevision;
public long getSourceRevision() { return sourceRevision; }
public void setSourceRevision(long value) { this.sourceRevision=value; }
}
