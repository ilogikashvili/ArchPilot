package com.ilia.gikashvili.archmain.template.entity;
import jakarta.persistence.*;
import java.util.UUID;
import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
@Entity
@Table(name="template_documents")
public class ArchitectureTemplate extends BaseEntity {
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
private UUID ownerId;
public UUID getOwnerId() { return ownerId; }
public void setOwnerId(UUID value) { this.ownerId=value; }
private boolean builtIn;
public boolean getBuiltIn() { return builtIn; }
public void setBuiltIn(boolean value) { this.builtIn=value; }
}
