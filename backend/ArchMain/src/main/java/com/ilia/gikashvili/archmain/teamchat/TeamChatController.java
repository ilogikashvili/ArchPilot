package com.ilia.gikashvili.archmain.teamchat;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.ai.AiProviderKeyRepository;
import com.ilia.gikashvili.archmain.common.exception.*;
import com.ilia.gikashvili.archmain.realtime.ProjectSocket;
import com.ilia.gikashvili.archmain.security.*;
import com.ilia.gikashvili.archmain.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Team chat: humans online, API agents and AI teammates in one channel per project.
 *  GET    /projects/{id}/team-agents                 list AI teammates
 *  POST   /projects/{id}/team-agents                 add one {name, role, persona, keyId, autoReply}
 *  PATCH  /projects/{id}/team-agents/{agentId}       update
 *  DELETE /projects/{id}/team-agents/{agentId}       remove
 *  GET    /projects/{id}/chat                        {messages, agents, online, discussionRunning}
 *  POST   /projects/{id}/chat/messages               {content}; "@Handle" / "@all" asks AI teammates
 *  POST   /projects/{id}/chat/discussions            {topic, agentIds?, rounds}
 *  POST   /projects/{id}/chat/discussions/stop
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class TeamChatController {
    private final TeamAgentRepository agents; private final TeamChatService chat; private final ProjectAccessService access;
    private final AiProviderKeyRepository keys; private final ProjectSocket socket; private final UserRepository users;
    public TeamChatController(TeamAgentRepository a, TeamChatService c, ProjectAccessService p, AiProviderKeyRepository k, ProjectSocket s, UserRepository u) {
        agents = a; chat = c; access = p; keys = k; socket = s; users = u;
    }

    public record AgentRequest(@NotBlank @Size(max = 60) String name, @Size(max = 120) String role, @Size(max = 4000) String persona,
                               UUID keyId, Boolean autoReply, @Size(max = 20) String color) {}
    public record MessageRequest(@NotBlank @Size(max = 8000) String content) {}
    public record DiscussionRequest(@NotBlank @Size(max = 2000) String topic, List<UUID> agentIds, Integer rounds) {}

    private TeamAgent find(UUID pid, UUID id) { return agents.findByIdAndProjectId(id, pid).orElseThrow(() -> new NotFoundException("Team agent", id)); }

    private void apply(TeamAgent a, AgentRequest r, UUID userId, UUID pid) {
        if (r.name() != null) {
            String handle = TeamChatService.handleOf(r.name().trim());
            boolean taken = agents.findByProjectIdOrderByCreatedAtAsc(pid).stream().anyMatch(o -> !o.id.equals(a.id) && o.handle.equalsIgnoreCase(handle));
            if (taken) throw new ValidationException("Another teammate already uses @" + handle);
            a.name = r.name().trim(); a.handle = handle;
        }
        if (r.role() != null) a.role = r.role().trim();
        if (r.persona() != null) a.persona = r.persona().trim();
        if (r.autoReply() != null) a.autoReply = r.autoReply();
        if (r.color() != null) a.color = r.color();
        if (r.keyId() != null) {
            var k = keys.findByIdAndUserId(r.keyId(), userId).orElseThrow(() -> new ValidationException("Choose one of your own saved AI keys"));
            a.keyId = k.id; a.ownerUserId = userId; a.providerLabel = k.provider + " · " + k.model;
            var u = users.findById(userId).orElseThrow();
            a.ownerName = u.getDisplayName() == null ? u.getEmail() : u.getDisplayName();
        }
    }

    @GetMapping("/team-agents")
    public List<TeamAgent> list(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId) {
        access.require(projectId, u.id(), Permission.READ); return agents.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    @PostMapping("/team-agents") @ResponseStatus(HttpStatus.CREATED)
    public TeamAgent create(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId, @Valid @RequestBody AgentRequest r) {
        access.require(projectId, u.id(), Permission.EDIT);
        if (r.keyId() == null) throw new ValidationException("Choose which saved AI key (or Ollama model) this teammate uses");
        if (agents.findByProjectIdOrderByCreatedAtAsc(projectId).size() >= 12) throw new ValidationException("A project can have at most 12 AI teammates");
        TeamAgent a = new TeamAgent(); a.projectId = projectId; apply(a, r, u.id(), projectId);
        a = agents.save(a);
        chat.system(projectId, "@" + a.handle + " (" + (a.role == null || a.role.isBlank() ? "AI teammate" : a.role) + ") joined the team chat.", null);
        return a;
    }

    @PatchMapping("/team-agents/{agentId}")
    public TeamAgent update(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId, @PathVariable UUID agentId, @Valid @RequestBody AgentRequest r) {
        access.require(projectId, u.id(), Permission.EDIT);
        TeamAgent a = find(projectId, agentId); apply(a, r, u.id(), projectId); return agents.save(a);
    }

    @DeleteMapping("/team-agents/{agentId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId, @PathVariable UUID agentId) {
        access.require(projectId, u.id(), Permission.EDIT);
        TeamAgent a = find(projectId, agentId); agents.delete(a);
        chat.system(projectId, "@" + a.handle + " left the team chat.", null);
    }

    @GetMapping("/chat")
    public Map<String, Object> state(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId) {
        access.require(projectId, u.id(), Permission.READ);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("messages", chat.history(projectId));
        m.put("agents", agents.findByProjectIdOrderByCreatedAtAsc(projectId));
        m.put("online", socket.presence(projectId));
        m.put("discussionRunning", chat.discussionRunning(projectId));
        return m;
    }

    @PostMapping("/chat/messages") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> post(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId, @Valid @RequestBody MessageRequest r) {
        access.require(projectId, u.id(), Permission.READ);
        return chat.post(projectId, u.id(), r.content());
    }

    @PostMapping("/chat/discussions") @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> discuss(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId, @Valid @RequestBody DiscussionRequest r) {
        access.require(projectId, u.id(), Permission.EDIT);
        return chat.discuss(projectId, u.id(), r.topic().trim(), r.agentIds(), r.rounds() == null ? 2 : r.rounds());
    }

    @PostMapping("/chat/discussions/stop")
    public Map<String, Object> stop(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId) {
        access.require(projectId, u.id(), Permission.EDIT);
        return Map.of("stopped", chat.stopDiscussion(projectId));
    }
}
