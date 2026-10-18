package com.ilia.gikashvili.archmain.agent.service;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.agent.entity.AgentToken;
import com.ilia.gikashvili.archmain.agent.repository.AgentTokenRepository;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import com.ilia.gikashvili.archmain.common.exception.ValidationException;
import com.ilia.gikashvili.archmain.user.entity.User;
import com.ilia.gikashvili.archmain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class AgentTokenService {
    public static final String TOKEN_PREFIX = "amk_";
    private static final Set<ProjectRole> ALLOWED_ROLES = EnumSet.of(ProjectRole.EDITOR, ProjectRole.REVIEWER, ProjectRole.VIEWER);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AgentTokenRepository tokens;
    private final UserRepository users;
    private final ProjectAccessService access;

    public AgentTokenService(AgentTokenRepository tokens, UserRepository users, ProjectAccessService access) {
        this.tokens = tokens; this.users = users; this.access = access;
    }

    public record Created(AgentToken token, String plaintext) {}

    /** Only project owners/admins (MANAGE_MEMBERS) can connect an agent. */
    @Transactional
    public Created create(UUID projectId, UUID callerId, String name, String provider, ProjectRole role) {
        access.require(projectId, callerId, Permission.MANAGE_MEMBERS);
        ProjectRole r = role == null ? ProjectRole.EDITOR : role;
        if (!ALLOWED_ROLES.contains(r)) throw new ValidationException("Agents can only be EDITOR, REVIEWER or VIEWER");
        if (name == null || name.isBlank()) throw new ValidationException("Agent name is required");

        User agentUser = new User();
        agentUser.setEmail("agent-" + UUID.randomUUID() + "@agents.archmain.local");
        agentUser.setDisplayName(name.trim() + (provider == null || provider.isBlank() ? "" : " (" + provider.trim() + ")"));
        agentUser.setPasswordHash("!agent-no-password");
        agentUser.setEmailVerified(true);
        agentUser.setRole("AGENT");
        agentUser = users.save(agentUser);
        access.addMember(projectId, agentUser.getId(), r);

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String plaintext = TOKEN_PREFIX + HexFormat.of().formatHex(raw);

        AgentToken t = new AgentToken();
        t.setProjectId(projectId);
        t.setAgentUserId(agentUser.getId());
        t.setName(name.trim());
        t.setProvider(provider);
        t.setRole(r);
        t.setCreatedBy(callerId);
        t.setTokenHash(hash(plaintext));
        t.setTokenPrefix(plaintext.substring(0, 12));
        return new Created(tokens.save(t), plaintext);
    }

    @Transactional(readOnly = true)
    public List<AgentToken> list(UUID projectId, UUID callerId) {
        access.require(projectId, callerId, Permission.READ);
        return tokens.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /** Revoking disconnects the agent immediately and removes its project membership. */
    @Transactional
    public void revoke(UUID projectId, UUID tokenId, UUID callerId) {
        access.require(projectId, callerId, Permission.MANAGE_MEMBERS);
        AgentToken t = tokens.findById(tokenId).filter(x -> x.getProjectId().equals(projectId))
                .orElseThrow(() -> new NotFoundException("Agent", tokenId));
        t.setRevoked(true);
        tokens.save(t);
        access.removeMember(projectId, t.getAgentUserId(), callerId);
    }

    /** Resolve a plaintext token to an active agent. */
    @Transactional
    public Optional<AgentToken> authenticate(String plaintext) {
        if (plaintext == null || !plaintext.startsWith(TOKEN_PREFIX)) return Optional.empty();
        return tokens.findByTokenHash(hash(plaintext)).filter(t -> !t.isRevoked()).map(t -> {
            if (t.getLastUsedAt() == null || t.getLastUsedAt().isBefore(Instant.now().minusSeconds(30))) {
                t.setLastUsedAt(Instant.now());
                tokens.save(t);
            }
            return t;
        });
    }

    @Transactional(readOnly = true)
    public Optional<AgentToken> findByAgentUser(UUID userId) {
        return tokens.findByAgentUserId(userId);
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
