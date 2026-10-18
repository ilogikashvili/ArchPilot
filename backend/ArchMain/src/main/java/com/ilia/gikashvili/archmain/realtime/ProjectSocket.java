package com.ilia.gikashvili.archmain.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live collaboration channel per project.
 *
 * Server -> client events: user.joined, user.left, presence, cursor.moved, selection.changed,
 * node.updated, edge.updated, operations.applied, architecture.updated.
 * Client -> server messages: cursor.moved {x,y}, selection.changed {id}, node.updated, edge.updated (transient drag previews).
 * Durable edits go through POST /projects/{id}/architecture/operations and are broadcast as operations.applied.
 */
@Component
public class ProjectSocket extends TextWebSocketHandler {
    private static final Set<String> VIEWER_MESSAGES = Set.of("cursor.moved", "selection.changed");
    private static final Set<String> EDITOR_MESSAGES = Set.of("node.updated", "edge.updated");

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper json;

    public ProjectSocket(ObjectMapper json) { this.json = json; }

    private UUID project(WebSocketSession s) { return (UUID) s.getAttributes().get("projectId"); }

    public List<Map<String, Object>> presence(UUID pid) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        sessions.values().stream().filter(s -> s.isOpen() && pid.equals(project(s)))
                .forEach(s -> out.put(s.getId(), user(s)));
        return new ArrayList<>(out.values());
    }

    private Map<String, Object> user(WebSocketSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("connectionId", s.getId());
        m.put("userId", s.getAttributes().get("userId"));
        m.put("email", s.getAttributes().get("email"));
        m.put("name", s.getAttributes().get("name"));
        m.put("agent", s.getAttributes().get("agent"));
        return m;
    }

    private void broadcast(UUID pid, Map<String, Object> event, String excludeSessionId) {
        String text;
        try { text = json.writeValueAsString(event); } catch (Exception e) { throw new IllegalStateException(e); }
        for (WebSocketSession s : sessions.values()) {
            if (s.getId().equals(excludeSessionId) || !pid.equals(project(s)) || !s.isOpen()) continue;
            try {
                synchronized (s) { s.sendMessage(new TextMessage(text)); }
            } catch (Exception e) {
                sessions.remove(s.getId());
                try { s.close(); } catch (Exception ignored) {}
            }
        }
    }

    /** Push a server event (e.g. chat.message, chat.typing) to everyone connected to the project. */
    public void publish(UUID pid, Map<String, Object> event) { broadcast(pid, event, null); }

    private void send(WebSocketSession s, Map<String, Object> event) {
        try { synchronized (s) { s.sendMessage(new TextMessage(json.writeValueAsString(event))); } } catch (Exception ignored) {}
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession s) {
        sessions.put(s.getId(), s);
        send(s, Map.of("type", "presence", "self", user(s), "users", presence(project(s))));
        broadcast(project(s), Map.of("type", "user.joined", "projectId", project(s), "user", user(s)), s.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
        sessions.remove(s.getId());
        broadcast(project(s), Map.of("type", "user.left", "projectId", project(s), "user", user(s)), null);
    }

    @Override
    protected void handleTextMessage(WebSocketSession s, TextMessage message) throws Exception {
        JsonNode node;
        try { node = json.readTree(message.getPayload()); } catch (Exception e) { s.close(CloseStatus.BAD_DATA); return; }
        String type = node.path("type").asText();
        boolean canEdit = Boolean.TRUE.equals(s.getAttributes().get("canEdit"));
        if (!VIEWER_MESSAGES.contains(type) && !(EDITOR_MESSAGES.contains(type) && canEdit)) {
            send(s, Map.of("type", "error", "message", "Message type not allowed: " + type));
            return;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", type);
        out.put("payload", node.path("payload"));
        out.put("user", user(s));
        broadcast(project(s), out, s.getId());
    }

    @TransactionalEventListener
    public void architectureUpdated(ArchitectureUpdated e) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "architecture.updated");
        data.put("projectId", e.projectId());
        data.put("branchId", e.branchId());
        data.put("userId", e.userId());
        broadcast(e.projectId(), data, null);
    }

    @TransactionalEventListener
    public void operationsApplied(OperationsApplied e) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "operations.applied");
        data.put("projectId", e.projectId());
        data.put("userId", e.userId());
        data.put("clientId", e.clientId());
        data.put("revision", e.revision());
        data.put("operations", e.operations());
        broadcast(e.projectId(), data, null);
    }
}
