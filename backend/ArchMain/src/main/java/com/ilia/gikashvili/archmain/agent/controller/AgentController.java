package com.ilia.gikashvili.archmain.agent.controller;

import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import com.ilia.gikashvili.archmain.agent.entity.AgentToken;
import com.ilia.gikashvili.archmain.agent.service.AgentTokenService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.common.exception.ForbiddenException;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping(ApiVersion.V1)
public class AgentController {

    private final AgentTokenService service;

    public AgentController(AgentTokenService service) { this.service = service; }

    public record CreateAgentRequest(String name, String provider, ProjectRole role) {}
    public record AgentResponse(UUID id, UUID agentUserId, String name, String provider, ProjectRole role,
                                String tokenPrefix, Instant createdAt, Instant lastUsedAt, boolean revoked, String token) {
        static AgentResponse of(AgentToken t, String token) {
            return new AgentResponse(t.getId(), t.getAgentUserId(), t.getName(), t.getProvider(), t.getRole(),
                    t.getTokenPrefix(), t.getCreatedAt(), t.getLastUsedAt(), t.isRevoked(), token);
        }
    }

    /** GET /projects/{projectId}/agents — connected AI models. */
    @GetMapping("/projects/{projectId}/agents")
    public List<AgentResponse> list(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId) {
        return service.list(projectId, u.id()).stream().map(t -> AgentResponse.of(t, null)).toList();
    }

    /** POST /projects/{projectId}/agents — owner/admin connects a model; plaintext token is returned once. */
    @PostMapping("/projects/{projectId}/agents")
    public ResponseEntity<AgentResponse> create(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId,
                                                @RequestBody CreateAgentRequest r) {
        var created = service.create(projectId, u.id(), r.name(), r.provider(), r.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(AgentResponse.of(created.token(), created.plaintext()));
    }

    /** DELETE /projects/{projectId}/agents/{agentId} — revoke access. */
    @DeleteMapping("/projects/{projectId}/agents/{agentId}")
    public ResponseEntity<Void> revoke(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId, @PathVariable UUID agentId) {
        service.revoke(projectId, agentId, u.id());
        return ResponseEntity.noContent().build();
    }

    /** GET /agent/me — lets a connected model discover its project, role and the editing API. */
    @GetMapping("/agent/me")
    public Map<String, Object> me(@CurrentUser AuthenticatedUser u) {
        AgentToken t = service.findByAgentUser(u.id()).orElseThrow(() -> new ForbiddenException("Not an agent token"));
        String p = "/api/v1/projects/" + t.getProjectId();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agentId", t.getId());
        out.put("name", t.getName());
        out.put("provider", t.getProvider());
        out.put("projectId", t.getProjectId());
        out.put("role", t.getRole());
        out.put("endpoints", Map.of(
                "readArchitecture", "GET " + p + "/architecture",
                "applyOperations", "POST " + p + "/architecture/operations",
                "componentTypes", "GET /api/v1/architecture/component-types",
                "liveSocket", "WS /ws/projects/" + t.getProjectId() + "?token=<this token>"));
        out.put("operations", List.of(
                "{\"op\":\"node.add\",\"node\":{\"id\":\"n1\",\"type\":\"service\",\"label\":\"Orders\",\"description\":\"\",\"x\":100,\"y\":100,\"properties\":{}}}",
                "{\"op\":\"node.update\",\"id\":\"n1\",\"patch\":{\"label\":\"Order Service\",\"x\":200}}",
                "{\"op\":\"node.remove\",\"id\":\"n1\"}",
                "{\"op\":\"edge.add\",\"edge\":{\"id\":\"e1\",\"type\":\"sync\",\"sourceId\":\"n1\",\"targetId\":\"n2\",\"label\":\"REST\",\"properties\":{}}}",
                "{\"op\":\"edge.update\",\"id\":\"e1\",\"patch\":{\"label\":\"gRPC\"}}",
                "{\"op\":\"edge.remove\",\"id\":\"e1\"}"));
        return out;
    }
}
