package com.ilia.gikashvili.archmain.teamchat;

import com.ilia.gikashvili.archmain.ai.*;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureService;
import com.ilia.gikashvili.archmain.architecture.model.*;
import com.ilia.gikashvili.archmain.agent.repository.AgentTokenRepository;
import com.ilia.gikashvili.archmain.realtime.ProjectSocket;
import com.ilia.gikashvili.archmain.user.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Team chat shared by humans online in a project, API agents (agent tokens) and AI teammates.
 * AI teammates answer when @mentioned (or @all / @team / @agents), may mention each other (bounded chain depth),
 * and can hold a moderated round-robin discussion on a topic. Every message is pushed live as "chat.message".
 */
@Service
public class TeamChatService {
    private static final Logger log = LoggerFactory.getLogger(TeamChatService.class);
    static final int MAX_CHAIN_DEPTH = 3;
    private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9_\\-]+)");
    private static final Set<String> EVERYONE = Set.of("all", "team", "agents", "everyone");

    private final TeamAgentRepository agents; private final ChatMessageRepository messages;
    private final AiProviderKeyRepository keys; private final AiProviderClient provider;
    private final ProjectSocket socket; private final UserRepository users; private final AgentTokenRepository apiAgents;
    private final ArchitectureService architecture;
    private final ExecutorService pool = Executors.newFixedThreadPool(4, r -> { Thread t = new Thread(r, "team-chat"); t.setDaemon(true); return t; });
    private final Map<UUID, UUID> runningDiscussion = new ConcurrentHashMap<>();
    /** AI teammates already queued or answering; a second mention while busy is skipped to avoid duplicate replies. */
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
    private void queue(TeamAgent a, int depth) { if (busy.add(a.id)) pool.submit(() -> { try { reply(a, depth, null); } finally { busy.remove(a.id); } }); }

    public TeamChatService(TeamAgentRepository a, ChatMessageRepository m, AiProviderKeyRepository k, AiProviderClient p, ProjectSocket s,
                           UserRepository u, AgentTokenRepository at, ArchitectureService arch) {
        agents = a; messages = m; keys = k; provider = p; socket = s; users = u; apiAgents = at; architecture = arch;
    }
    @PreDestroy void stop() { pool.shutdownNow(); }

    public static String handleOf(String name) { String h = name.replaceAll("[^A-Za-z0-9_\\-]", ""); return h.isEmpty() ? "Agent" : h; }

    public Map<String, Object> toMap(ChatMessage m) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", m.id); x.put("projectId", m.projectId); x.put("authorType", m.authorType); x.put("authorId", m.authorId);
        x.put("authorName", m.authorName); x.put("content", m.content); x.put("discussionId", m.discussionId); x.put("createdAt", m.createdAt);
        return x;
    }

    public List<Map<String, Object>> history(UUID pid) {
        List<ChatMessage> list = new ArrayList<>(messages.findTop100ByProjectIdOrderByCreatedAtDesc(pid));
        Collections.reverse(list); return list.stream().map(this::toMap).toList();
    }

    private ChatMessage save(UUID pid, ChatMessage.AuthorType type, String authorId, String name, String content, UUID discussion) {
        ChatMessage m = new ChatMessage(); m.projectId = pid; m.authorType = type; m.authorId = authorId; m.authorName = name;
        m.content = content.length() > 8000 ? content.substring(0, 8000) + "…" : content; m.discussionId = discussion;
        messages.save(m);
        socket.publish(pid, Map.of("type", "chat.message", "message", toMap(m)));
        return m;
    }
    private void typing(UUID pid, TeamAgent a, boolean on) {
        socket.publish(pid, Map.of("type", "chat.typing", "agentId", a.id.toString(), "name", a.name, "typing", on));
    }
    public void system(UUID pid, String text, UUID discussion) { save(pid, ChatMessage.AuthorType.SYSTEM, null, "System", text, discussion); }

    /** A human (or API agent) posts; mentioned AI teammates reply asynchronously. */
    public Map<String, Object> post(UUID pid, UUID userId, String content) {
        var user = users.findById(userId).orElseThrow();
        boolean api = apiAgents.findByAgentUserId(userId).isPresent();
        String name = user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getEmail() : user.getDisplayName();
        ChatMessage m = save(pid, api ? ChatMessage.AuthorType.API_AGENT : ChatMessage.AuthorType.USER, userId.toString(), name, content.trim(), null);
        List<TeamAgent> targets = targets(pid, content, null);
        if (targets.isEmpty() && !api) targets = agents.findByProjectIdOrderByCreatedAtAsc(pid).stream().filter(a -> a.autoReply).toList();
        for (TeamAgent a : targets) queue(a, 1);
        return toMap(m);
    }

    List<TeamAgent> targets(UUID pid, String content, UUID exclude) {
        Set<String> mentioned = new HashSet<>(); Matcher mt = MENTION.matcher(content);
        while (mt.find()) mentioned.add(mt.group(1).toLowerCase(Locale.ROOT));
        if (mentioned.isEmpty()) return List.of();
        boolean all = mentioned.stream().anyMatch(EVERYONE::contains);
        return agents.findByProjectIdOrderByCreatedAtAsc(pid).stream()
                .filter(a -> !a.id.equals(exclude))
                .filter(a -> all || mentioned.contains(a.handle.toLowerCase(Locale.ROOT))).toList();
    }

    /** Runs one AI turn. Returns the saved message, or null when the provider failed (an error notice is posted instead). */
    ChatMessage reply(TeamAgent a, int depth, UUID discussion) {
        UUID pid = a.projectId;
        typing(pid, a, true);
        try {
            AiProviderKey key = keys.findById(a.keyId).orElseThrow(() -> new IllegalStateException("The AI key for " + a.name + " was deleted"));
            String text = provider.call(key, systemPrompt(a, discussion != null), context(a), false).trim();
            text = text.replaceFirst("^(?i)\\**" + Pattern.quote(a.name) + "\\**\\s*:\\s*", "");
            if (text.isBlank()) text = "(no answer)";
            ChatMessage m = save(pid, ChatMessage.AuthorType.AI_AGENT, a.id.toString(), a.name, text, discussion);
            if (discussion == null && depth < MAX_CHAIN_DEPTH)
                for (TeamAgent next : targets(pid, text, a.id)) queue(next, depth + 1);
            return m;
        } catch (Exception e) {
            log.warn("Team agent {} failed: {}", a.name, e.toString());
            String msg = e instanceof IllegalStateException ? e.getMessage() : "AI provider request failed";
            system(pid, a.name + " could not answer: " + msg, discussion);
            return null;
        } finally { typing(pid, a, false); }
    }

    private String systemPrompt(TeamAgent a, boolean discussion) {
        List<TeamAgent> team = agents.findByProjectIdOrderByCreatedAtAsc(a.projectId);
        Set<String> online = new LinkedHashSet<>();
        for (var p : socket.presence(a.projectId)) online.add(String.valueOf(p.get("name")) + (Boolean.TRUE.equals(p.get("agent")) ? " (API agent)" : " (human)"));
        StringBuilder s = new StringBuilder();
        s.append("You are ").append(a.name).append(" (@").append(a.handle).append(")");
        if (a.role != null && !a.role.isBlank()) s.append(", acting as ").append(a.role);
        s.append(", a member of a software architecture team chat.\n");
        if (a.persona != null && !a.persona.isBlank()) s.append("Persona and instructions: ").append(a.persona).append("\n");
        s.append("Humans and API agents online now: ").append(online.isEmpty() ? "nobody" : String.join(", ", online)).append(".\n");
        s.append("AI teammates: ");
        for (TeamAgent t : team) s.append("@").append(t.handle).append(" (").append(t.role == null ? "generalist" : t.role).append(") ");
        s.append("\nRules: reply in plain text, at most 150 words, as yourself only (never write other people's lines). ")
         .append("Be concrete about the architecture shown. Disagree when you have reasons. ")
         .append(discussion ? "This is a moderated discussion: build on or challenge the previous points and add something new. Do not @mention anyone."
                            : "Mention a teammate with @handle only when you truly need their specific input.");
        return s.toString();
    }

    private String context(TeamAgent a) {
        StringBuilder s = new StringBuilder();
        try {
            ArchitectureDocument d = architecture.getDocumentForSystem(a.projectId);
            s.append("Current architecture:\n");
            if (d.nodes() != null) for (NodeModel n : d.nodes()) s.append("- [").append(n.type()).append("] ").append(n.label()).append(" (id ").append(n.id()).append(")\n");
            if (d.edges() != null) for (EdgeModel e : d.edges()) s.append("- ").append(e.sourceId()).append(" -> ").append(e.targetId()).append(" ").append(e.type() == null ? "" : e.type()).append(e.label() == null ? "" : " \"" + e.label() + "\"").append("\n");
        } catch (Exception e) { s.append("(architecture unavailable)\n"); }
        List<ChatMessage> recent = new ArrayList<>(messages.findTop30ByProjectIdOrderByCreatedAtDesc(a.projectId));
        Collections.reverse(recent);
        s.append("\nChat so far (oldest first):\n");
        for (ChatMessage m : recent) s.append(m.authorName).append(m.authorType == ChatMessage.AuthorType.AI_AGENT ? " [AI]" : m.authorType == ChatMessage.AuthorType.SYSTEM ? "" : "").append(": ").append(m.content).append("\n");
        s.append("\nNow write ").append(a.name).append("'s next message.");
        return s.toString();
    }

    /** Round-robin discussion between the chosen AI teammates; humans can keep chatting meanwhile. */
    public Map<String, Object> discuss(UUID pid, UUID userId, String topic, List<UUID> agentIds, int rounds) {
        List<TeamAgent> all = agents.findByProjectIdOrderByCreatedAtAsc(pid);
        List<TeamAgent> chosen = agentIds == null || agentIds.isEmpty() ? all : all.stream().filter(a -> agentIds.contains(a.id)).toList();
        if (chosen.size() < 2) throw new com.ilia.gikashvili.archmain.common.exception.ValidationException("Pick at least two AI teammates for a discussion");
        if (runningDiscussion.containsKey(pid)) throw new com.ilia.gikashvili.archmain.common.exception.ValidationException("A discussion is already running in this project");
        UUID id = UUID.randomUUID(); int r = Math.max(1, Math.min(rounds, 5));
        runningDiscussion.put(pid, id);
        var user = users.findById(userId).orElseThrow();
        String who = user.getDisplayName() == null ? user.getEmail() : user.getDisplayName();
        system(pid, who + " started a discussion (" + r + " round" + (r == 1 ? "" : "s") + ") between " + String.join(", ", chosen.stream().map(a -> "@" + a.handle).toList()) + ": " + topic, id);
        pool.submit(() -> {
            try {
                outer:
                for (int round = 1; round <= r; round++)
                    for (TeamAgent a : chosen) {
                        if (!id.equals(runningDiscussion.get(pid))) break outer;
                        reply(a, MAX_CHAIN_DEPTH, id);
                    }
                system(pid, id.equals(runningDiscussion.get(pid)) ? "Discussion finished." : "Discussion stopped.", id);
            } finally { runningDiscussion.remove(pid, id); }
        });
        return Map.of("discussionId", id, "rounds", r, "agents", chosen.stream().map(a -> a.handle).toList());
    }

    public boolean stopDiscussion(UUID pid) { return runningDiscussion.remove(pid) != null; }
    public boolean discussionRunning(UUID pid) { return runningDiscussion.containsKey(pid); }
}
