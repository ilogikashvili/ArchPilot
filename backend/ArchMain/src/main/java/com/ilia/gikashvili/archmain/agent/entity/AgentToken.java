package com.ilia.gikashvili.archmain.agent.entity;

import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A project-scoped access token for an external AI model (GPT, Claude, Copilot, local models...).
 * Each token is backed by a synthetic user that is a member of exactly one project, so all
 * regular permission checks, presence and audit logic apply to agents exactly like to people.
 */
@Entity
@Table(name = "agent_tokens")
public class AgentToken {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(nullable = false) private UUID projectId;
    @Column(nullable = false) private UUID agentUserId;
    @Column(nullable = false) private String name;
    private String provider;
    @Column(nullable = false, unique = true) private String tokenHash;
    @Column(nullable = false) private String tokenPrefix;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProjectRole role;
    @Column(nullable = false) private UUID createdBy;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    private Instant lastUsedAt;
    private boolean revoked;

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID v) { projectId = v; }
    public UUID getAgentUserId() { return agentUserId; }
    public void setAgentUserId(UUID v) { agentUserId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getProvider() { return provider; }
    public void setProvider(String v) { provider = v; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { tokenHash = v; }
    public String getTokenPrefix() { return tokenPrefix; }
    public void setTokenPrefix(String v) { tokenPrefix = v; }
    public ProjectRole getRole() { return role; }
    public void setRole(ProjectRole v) { role = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant v) { lastUsedAt = v; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean v) { revoked = v; }
}
