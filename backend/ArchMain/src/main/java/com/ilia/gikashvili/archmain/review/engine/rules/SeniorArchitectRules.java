package com.ilia.gikashvili.archmain.review.engine.rules;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.model.EdgeModel;
import com.ilia.gikashvili.archmain.architecture.model.NodeModel;
import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Checks a senior engineer would raise when reviewing a junior's system design:
 * layering violations, coupling, cycles, failure propagation, data ownership and missing pieces.
 * Each finding explains the concrete risk ("why it will hurt") and the fix.
 */
@Component
public class SeniorArchitectRules implements ReviewRule {

    private record G(Map<String, NodeModel> byId, List<EdgeModel> edges) {
        List<EdgeModel> out(String id) { return edges.stream().filter(e -> id.equals(e.sourceId())).toList(); }
        List<EdgeModel> in(String id) { return edges.stream().filter(e -> id.equals(e.targetId())).toList(); }
        NodeModel n(String id) { return byId.get(id); }
    }

    static String t(NodeModel n) { return n.type() == null ? "" : n.type().toLowerCase(); }
    static String l(NodeModel n) { return n.label() == null ? "" : n.label().toLowerCase(); }
    static boolean client(NodeModel n) { String t = t(n); return t.equals("web") || t.contains("mobile") || t.contains("client") || t.contains("frontend"); }
    static boolean gateway(NodeModel n) { return t(n).equals("api") || t(n).contains("gateway") || l(n).contains("gateway") || l(n).contains("load balancer"); }
    static boolean db(NodeModel n) { String t = t(n); return t.contains("database") || t.equals("db") || t.contains("sql"); }
    static boolean cache(NodeModel n) { return t(n).contains("cache") || l(n).contains("redis"); }
    static boolean queue(NodeModel n) { String t = t(n); return t.contains("queue") || t.contains("broker") || t.contains("topic"); }
    static boolean external(NodeModel n) { return t(n).contains("external"); }
    static boolean service(NodeModel n) { return t(n).contains("service") || t(n).equals("worker") || t(n).equals("function"); }
    static boolean sync(EdgeModel e) { String t = e.type() == null ? "sync" : e.type().toLowerCase(); return !(t.equals("event") || t.equals("async")); }
    static String name(NodeModel n) { return n.label() == null || n.label().isBlank() ? n.id() : n.label(); }
    static boolean prop(Object holder, String key) {
        if (holder instanceof Map<?, ?> m) { Object v = m.get(key); return v != null && !String.valueOf(v).isBlank() && !"false".equals(String.valueOf(v)); }
        return false;
    }

    @Override
    public List<ReviewFinding> check(UUID reviewId, UUID projectId, ArchitectureDocument doc) {
        List<ReviewFinding> f = new ArrayList<>();
        List<NodeModel> nodes = doc.nodes() == null ? List.of() : doc.nodes().stream().filter(n -> n.id() != null).toList();
        List<EdgeModel> edges = doc.edges() == null ? List.of() : doc.edges().stream().filter(e -> e.sourceId() != null && e.targetId() != null).toList();
        Map<String, NodeModel> byId = new LinkedHashMap<>(); nodes.forEach(n -> byId.putIfAbsent(n.id(), n));
        G g = new G(byId, edges.stream().filter(e -> byId.containsKey(e.sourceId()) && byId.containsKey(e.targetId())).toList());
        java.util.function.Function<Object[], ReviewFinding> mk = a -> {
            ReviewFinding r = new ReviewFinding();
            r.setReviewId(reviewId); r.setProjectId(projectId);
            r.setSeverity((String) a[0]); r.setCategory((String) a[1]); r.setTitle((String) a[2]);
            r.setDescription((String) a[3]); r.setRecommendation((String) a[4]); r.setAffectedElementId((String) a[5]);
            return r;
        };

        if (nodes.isEmpty()) {
            f.add(mk.apply(new Object[]{"INFO", "COMPLETENESS", "Nothing to review yet", "The diagram is empty.", "Start with clients, an entry point, services and their data stores.", null}));
            return f;
        }

        // 1. Clients bypassing the backend
        for (EdgeModel e : g.edges()) {
            NodeModel s = g.n(e.sourceId()), t = g.n(e.targetId());
            if (client(s) && (db(t) || cache(t) || queue(t)))
                f.add(mk.apply(new Object[]{"CRITICAL", "SECURITY", "Client talks directly to " + name(t),
                        name(s) + " connects straight to a data store. Credentials ship to every browser/phone, there is no authorization layer and the schema becomes a public API you can never change.",
                        "Never expose data stores to clients. Put an API/service in between and let it enforce auth and validation.", e.id()}));
            else if (client(s) && service(t))
                f.add(mk.apply(new Object[]{"WARNING", "SECURITY", name(s) + " calls " + name(t) + " directly",
                        "Clients are coupled to internal service addresses; auth, rate limiting and CORS get re-implemented in every service.",
                        "Route clients through an API Gateway (or a BFF). Try the 'API Gateway' pattern from the patterns menu.", e.id()}));
        }

        // 2. Shared database (data ownership)
        for (NodeModel d : nodes.stream().filter(SeniorArchitectRules::db).toList()) {
            List<String> owners = g.in(d.id()).stream().map(e -> g.n(e.sourceId())).filter(SeniorArchitectRules::service).map(SeniorArchitectRules::name).distinct().toList();
            if (owners.size() >= 2)
                f.add(mk.apply(new Object[]{"CRITICAL", "DATA_OWNERSHIP", "Shared database: " + name(d) + " used by " + String.join(", ", owners),
                        "Multiple services writing the same schema is a distributed monolith: one migration breaks everyone, you cannot deploy independently and nobody owns data integrity.",
                        "Give each service its own schema/database and share data through APIs or events ('Database per Service').", d.id()}));
            if (g.in(d.id()).isEmpty())
                f.add(mk.apply(new Object[]{"INFO", "COMPLETENESS", name(d) + " is never used", "No component reads or writes this database.", "Connect it to the owning service or remove it.", d.id()}));
        }

        // 3. Dependency cycles
        Set<String> reported = new HashSet<>();
        for (List<String> cycle : cycles(g)) {
            String key = new TreeSet<>(cycle).toString();
            if (!reported.add(key)) continue;
            List<String> names = cycle.stream().map(id -> name(g.n(id))).toList();
            f.add(mk.apply(new Object[]{"CRITICAL", "COUPLING", "Circular dependency: " + String.join(" → ", names) + " → " + names.get(0),
                    "Cycles mean none of these can be deployed, tested or reasoned about alone, and synchronous cycles can deadlock or recurse under load.",
                    "Break the cycle: invert one dependency with an event, or extract the shared logic into its own service.", cycle.get(0)}));
        }

        // 4. Long synchronous chains (latency + cascading failure)
        for (NodeModel n : nodes) {
            if (!client(n) && !gateway(n)) continue;
            List<String> path = longestSync(g, n.id(), new LinkedHashSet<>());
            long hops = path.stream().filter(id -> service(g.n(id))).count();
            if (hops >= 3) {
                f.add(mk.apply(new Object[]{"WARNING", "RESILIENCE", "Synchronous chain of " + hops + " services from " + name(n),
                        "Path: " + String.join(" → ", path.stream().map(id -> name(g.n(id))).toList()) + ". Latencies add up and availability multiplies (3 × 99.9% ≈ 99.7%); one slow service stalls the whole request.",
                        "Make non-critical steps asynchronous (events), add timeouts + circuit breakers, or collapse chatty services.", n.id()}));
                break;
            }
        }

        // 5. God service / high fan-out
        for (NodeModel n : nodes.stream().filter(SeniorArchitectRules::service).toList()) {
            long fan = g.out(n.id()).stream().filter(e -> sync(e) && service(g.n(e.targetId()))).count();
            if (fan >= 3)
                f.add(mk.apply(new Object[]{"WARNING", "COUPLING", name(n) + " orchestrates " + fan + " services synchronously",
                        "This service knows too much and fails whenever any dependency fails. Partial failures leave data inconsistent with no rollback.",
                        "Use a Saga (orchestration with compensations) or event-driven choreography.", n.id()}));
            Object resp = n.properties() == null ? null : n.properties().get("responsibilities");
            if (resp instanceof Collection<?> c && c.size() >= 6)
                f.add(mk.apply(new Object[]{"WARNING", "COHESION", name(n) + " has " + c.size() + " responsibilities",
                        "A service doing everything is a monolith with network overhead; it will change for many unrelated reasons.",
                        "Split by bounded context; aim for one business capability per service.", n.id()}));
            if ((n.description() == null || n.description().isBlank()) && !prop(n.properties(), "technology"))
                f.add(mk.apply(new Object[]{"INFO", "DOCUMENTATION", name(n) + " has no description or technology",
                        "Reviewers and new team members cannot tell what this owns.", "Document its responsibility in one sentence and its tech stack.", n.id()}));
        }

        // 6. Synchronous calls to third parties without protection
        for (EdgeModel e : g.edges()) {
            NodeModel t = g.n(e.targetId());
            if (external(t) && sync(e) && !prop(e.properties(), "resilience"))
                f.add(mk.apply(new Object[]{"WARNING", "RESILIENCE", "Unprotected call to third party " + name(t),
                        "You do not control its uptime or latency. Without timeouts, retries with idempotency keys and a breaker, their outage becomes yours.",
                        "Add timeout + retry + circuit breaker, and an outbox/queue for anything that must eventually succeed (e.g. payments).", e.id()}));
        }

        // 7. Queues missing a producer or consumer
        for (NodeModel q : nodes.stream().filter(SeniorArchitectRules::queue).toList()) {
            if (g.in(q.id()).isEmpty() || g.out(q.id()).isEmpty())
                f.add(mk.apply(new Object[]{"WARNING", "MESSAGING", name(q) + " has no " + (g.in(q.id()).isEmpty() ? "producer" : "consumer"),
                        "Messages are either never sent or pile up forever.", "Connect a producer and a consumer, and plan a dead-letter queue for poison messages.", q.id()}));
        }

        // 8. Single points of failure on the entry path
        for (NodeModel n : nodes.stream().filter(SeniorArchitectRules::gateway).toList()) {
            Object r = n.properties() == null ? null : n.properties().get("replicas");
            if (!(r instanceof Number num && num.intValue() >= 2) && g.in(n.id()).stream().anyMatch(e -> client(g.n(e.sourceId()))))
                f.add(mk.apply(new Object[]{"WARNING", "AVAILABILITY", name(n) + " is a single point of failure",
                        "Every request passes through one instance; a restart or crash takes the whole product down.",
                        "Run at least 2 replicas behind a load balancer ('Load Balancer + Replicas' pattern).", n.id()}));
        }

        // 9. Read-heavy DB without cache
        boolean anyCache = nodes.stream().anyMatch(SeniorArchitectRules::cache);
        if (!anyCache) for (NodeModel d : nodes.stream().filter(SeniorArchitectRules::db).toList()) {
            if (g.in(d.id()).size() >= 2) {
                f.add(mk.apply(new Object[]{"INFO", "PERFORMANCE", "No caching layer in front of " + name(d),
                        "Every read hits the database; it will be the first thing to fall over under load.", "Add a cache-aside layer (Redis) for hot reads and define TTL/invalidation.", d.id()}));
                break;
            }
        }

        // 10. Orphans and missing basics
        for (NodeModel n : nodes) if (g.in(n.id()).isEmpty() && g.out(n.id()).isEmpty() && nodes.size() > 1)
            f.add(mk.apply(new Object[]{"INFO", "COMPLETENESS", name(n) + " is not connected", "It doesn't take part in any flow, so it is either dead or the diagram is incomplete.", "Connect it or delete it.", n.id()}));
        if (nodes.stream().anyMatch(SeniorArchitectRules::service) && nodes.stream().noneMatch(n -> l(n).contains("auth") || l(n).contains("identity") || l(n).contains("user") || gateway(n)))
            f.add(mk.apply(new Object[]{"WARNING", "SECURITY", "No authentication boundary",
                    "Nothing in the design verifies who the caller is. Each service will invent its own auth or skip it.",
                    "Add an identity provider/auth service and validate tokens at the gateway.", null}));
        if (nodes.stream().filter(SeniorArchitectRules::service).count() >= 3 && nodes.stream().noneMatch(n -> l(n).contains("monitor") || l(n).contains("log") || l(n).contains("trac") || l(n).contains("observ")))
            f.add(mk.apply(new Object[]{"INFO", "OPERABILITY", "No observability",
                    "With several services and no central logs/metrics/tracing, debugging production means guessing.",
                    "Add centralized logging, metrics and distributed tracing (e.g. OpenTelemetry).", null}));
        return f;
    }

    private List<List<String>> cycles(G g) {
        List<List<String>> out = new ArrayList<>();
        Map<String, Integer> state = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        for (String id : g.byId().keySet()) dfs(g, id, state, stack, out);
        return out;
    }

    private void dfs(G g, String id, Map<String, Integer> state, Deque<String> stack, List<List<String>> out) {
        Integer s = state.get(id);
        if (s != null && s == 2) return;
        if (s != null && s == 1) {
            List<String> path = new ArrayList<>(stack); Collections.reverse(path);
            int i = path.indexOf(id); if (i >= 0 && path.size() - i >= 2) out.add(new ArrayList<>(path.subList(i, path.size())));
            return;
        }
        if (db(g.n(id)) || cache(g.n(id))) { state.put(id, 2); return; }
        state.put(id, 1); stack.push(id);
        for (EdgeModel e : g.out(id)) if (sync(e)) dfs(g, e.targetId(), state, stack, out);
        stack.pop(); state.put(id, 2);
    }

    private List<String> longestSync(G g, String id, LinkedHashSet<String> seen) {
        if (!seen.add(id)) return List.of();
        List<String> best = List.of();
        for (EdgeModel e : g.out(id)) {
            if (!sync(e)) continue;
            List<String> p = longestSync(g, e.targetId(), seen);
            if (p.size() > best.size()) best = p;
        }
        seen.remove(id);
        List<String> r = new ArrayList<>(); r.add(id); r.addAll(best); return r;
    }
}
