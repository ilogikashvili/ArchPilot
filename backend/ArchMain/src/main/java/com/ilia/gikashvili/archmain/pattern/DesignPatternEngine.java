package com.ilia.gikashvili.archmain.pattern;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.model.EdgeModel;
import com.ilia.gikashvili.archmain.architecture.model.NodeModel;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Transforms an architecture document into one that follows a chosen design pattern.
 * Output is a list of standard diagram operations (node.add/update/remove, edge.add/update/remove),
 * so the result can be previewed, applied by the live editor (with undo) or applied server-side.
 */
@Component
public class DesignPatternEngine {

    public record PatternInfo(String id, String name, String category, String description, String whenToUse) {}

    public record Transformation(String patternId, boolean applicable, String summary, List<String> changes,
                                 List<Map<String, Object>> operations, List<String> highlightNodeIds,
                                 List<String> highlightEdgeIds) {}

    public static final List<PatternInfo> CATALOG = List.of(
            new PatternInfo("api-gateway", "API Gateway", "Integration",
                    "Clients call one entry point that routes, authenticates and rate-limits requests to services.",
                    "Clients talk directly to several services."),
            new PatternInfo("bff", "Backend for Frontend", "Integration",
                    "Each client type gets its own thin backend that shapes data for that screen.",
                    "Web and mobile clients need different payloads."),
            new PatternInfo("database-per-service", "Database per Service", "Data",
                    "Each service owns a private database; no service reads another's tables.",
                    "Several services share one database."),
            new PatternInfo("cache-aside", "Cache-Aside", "Performance",
                    "Services read from a cache first and fall back to the database, then populate the cache.",
                    "Read-heavy services hit the database on every request."),
            new PatternInfo("cqrs", "CQRS", "Data",
                    "Writes go to the write store; reads come from a separate read model kept in sync by events.",
                    "Read and write workloads have very different shapes or scale."),
            new PatternInfo("event-driven", "Event-Driven (Pub/Sub)", "Messaging",
                    "Services publish events to a broker instead of calling each other synchronously.",
                    "Services are chained with synchronous calls and fail together."),
            new PatternInfo("saga", "Saga (Orchestration)", "Messaging",
                    "A saga orchestrator coordinates a multi-service transaction with compensating steps.",
                    "One service drives a business transaction across several services."),
            new PatternInfo("circuit-breaker", "Circuit Breaker", "Resilience",
                    "Synchronous calls to other services are wrapped so failures trip fast instead of cascading.",
                    "Services call other services or third parties synchronously."),
            new PatternInfo("load-balancer", "Load Balancer + Replicas", "Scalability",
                    "Entry points sit behind a load balancer and services run as multiple replicas.",
                    "Single instances are single points of failure."),
            new PatternInfo("solid-srp", "S: Single Responsibility", "SOLID Principles",
                    "Each service has one reason to change. Services juggling data, integrations and other services are split by responsibility.",
                    "A service talks to many different kinds of components (databases, third parties, other services)."),
            new PatternInfo("solid-ocp", "O: Open/Closed", "SOLID Principles",
                    "Variants (payment providers, notifiers, storage backends) plug into a strategy/extension point instead of being hard-wired.",
                    "A service calls two or more interchangeable components of the same kind."),
            new PatternInfo("solid-lsp", "L: Liskov Substitution", "SOLID Principles",
                    "Interchangeable components share one explicit contract so any implementation can replace another safely.",
                    "Several components of the same type are used in the same role."),
            new PatternInfo("solid-isp", "I: Interface Segregation", "SOLID Principles",
                    "A service used by many different consumers exposes a small, consumer-specific interface to each one.",
                    "Three or more different components call the same service."),
            new PatternInfo("solid-dip", "D: Dependency Inversion", "SOLID Principles",
                    "Services depend on abstractions (repository ports, adapters) instead of concrete databases and vendors.",
                    "Services talk directly to databases, caches, queues or third-party APIs."),
            new PatternInfo("solid-all", "SOLID (all five)", "SOLID Principles",
                    "Applies S, O, L, I and D in sequence.",
                    "You want a full SOLID clean-up pass.")
    );

    public List<PatternInfo> catalog() { return CATALOG; }

    public Transformation transform(String patternId, ArchitectureDocument doc) {
        Ctx c = new Ctx(doc);
        switch (patternId) {
            case "api-gateway" -> apiGateway(c);
            case "bff" -> bff(c);
            case "database-per-service" -> databasePerService(c);
            case "cache-aside" -> cacheAside(c);
            case "cqrs" -> cqrs(c);
            case "event-driven" -> eventDriven(c);
            case "saga" -> saga(c);
            case "circuit-breaker" -> circuitBreaker(c);
            case "load-balancer" -> loadBalancer(c);
            case "solid-srp" -> srp(c);
            case "solid-ocp" -> ocp(c);
            case "solid-lsp" -> lsp(c);
            case "solid-isp" -> isp(c);
            case "solid-dip" -> dip(c);
            case "solid-all" -> { srp(c); ocp(c); lsp(c); isp(c); dip(c); c.reason = c.ops.isEmpty() ? "The design already follows all five SOLID principles." : null; }
            default -> throw new NotFoundException("Design pattern", patternId);
        }
        PatternInfo info = CATALOG.stream().filter(p -> p.id().equals(patternId)).findFirst().orElseThrow();
        boolean applicable = !c.ops.isEmpty();
        String summary = applicable
                ? info.name() + ": " + c.changes.size() + " change" + (c.changes.size() == 1 ? "" : "s")
                : (c.reason != null ? c.reason : "Nothing to change: the design already follows " + info.name() + ".");
        return new Transformation(patternId, applicable, summary, c.changes, c.ops,
                new ArrayList<>(c.hlNodes), new ArrayList<>(c.hlEdges));
    }

    // ---------- classification ----------
    static String t(NodeModel n) { return n.type() == null ? "" : n.type().toLowerCase(); }
    static String l(NodeModel n) { return n.label() == null ? "" : n.label().toLowerCase(); }
    static boolean isClient(NodeModel n) { String t = t(n); return t.equals("web") || t.contains("mobile") || t.contains("client") || t.contains("frontend"); }
    static boolean isGateway(NodeModel n) { return t(n).contains("gateway") || l(n).contains("gateway") || (t(n).equals("api") && !l(n).contains("bff")); }
    static boolean isLb(NodeModel n) { return l(n).contains("load balancer") || t(n).contains("loadbalancer"); }
    static boolean isDb(NodeModel n) { String t = t(n); return t.contains("database") || t.equals("db") || t.contains("sql"); }
    static boolean isCache(NodeModel n) { return t(n).contains("cache") || l(n).contains("redis"); }
    static boolean isQueue(NodeModel n) { String t = t(n); return t.contains("queue") || t.contains("broker") || t.contains("topic") || l(n).contains("kafka") || l(n).contains("rabbit"); }
    static boolean isExternal(NodeModel n) { return t(n).contains("external"); }
    static boolean isService(NodeModel n) { return t(n).contains("service") || t(n).equals("worker") || t(n).equals("function"); }
    static boolean isSync(EdgeModel e) { String t = e.type() == null ? "sync" : e.type().toLowerCase(); return t.equals("sync") || t.equals("dependency") || t.equals("http") || t.equals("rest") || t.equals("grpc"); }

    // ---------- context / op builders ----------
    static final class Ctx {
        final List<NodeModel> nodes; final List<EdgeModel> edges; final Map<String, NodeModel> byId;
        final List<Map<String, Object>> ops = new ArrayList<>(); final List<String> changes = new ArrayList<>();
        final Set<String> hlNodes = new LinkedHashSet<>(); final Set<String> hlEdges = new LinkedHashSet<>();
        final Set<String> usedIds = new HashSet<>(); final Set<String> removedEdges = new HashSet<>();
        String reason;
        Ctx(ArchitectureDocument d) {
            nodes = d.nodes() == null ? List.of() : d.nodes().stream().filter(n -> n.id() != null).toList();
            edges = d.edges() == null ? List.of() : d.edges().stream().filter(e -> e.id() != null).toList();
            byId = nodes.stream().collect(Collectors.toMap(NodeModel::id, n -> n, (a, b) -> a, LinkedHashMap::new));
            nodes.forEach(n -> usedIds.add(n.id())); edges.forEach(e -> usedIds.add(e.id()));
        }
        NodeModel node(String id) { return byId.get(id); }
        List<EdgeModel> out(String id) { return edges.stream().filter(e -> id.equals(e.sourceId()) && !removedEdges.contains(e.id())).toList(); }
        List<EdgeModel> in(String id) { return edges.stream().filter(e -> id.equals(e.targetId()) && !removedEdges.contains(e.id())).toList(); }
        String newId(String base) {
            String slug = base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            String id = slug; int i = 2; while (usedIds.contains(id)) id = slug + "-" + i++;
            usedIds.add(id); return id;
        }
        String addNode(String type, String label, String description, double x, double y, Map<String, Object> props) {
            String id = newId(label);
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", id); n.put("type", type); n.put("label", label); n.put("description", description);
            n.put("x", Math.round(x)); n.put("y", Math.round(y)); n.put("properties", new LinkedHashMap<>(props));
            ops.add(Map.of("op", "node.add", "node", n)); hlNodes.add(id);
            return id;
        }
        String addEdge(String source, String target, String type, String label) {
            String id = newId("e-" + source + "-" + target);
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id", id); e.put("type", type); e.put("sourceId", source); e.put("targetId", target);
            if (label != null) e.put("label", label); e.put("properties", new LinkedHashMap<>());
            ops.add(Map.of("op", "edge.add", "edge", e)); hlEdges.add(id);
            return id;
        }
        void removeEdge(EdgeModel e) { if (removedEdges.add(e.id())) ops.add(Map.of("op", "edge.remove", "id", e.id())); }
        final Map<String, Map<String, Object>> pendingProps = new HashMap<>();
        @SuppressWarnings("unchecked")
        void updateNode(String id, Map<String, Object> patch) { if (patch.get("properties") instanceof Map<?, ?> pp) pendingProps.put(id, new LinkedHashMap<>((Map<String, Object>) pp)); ops.add(Map.of("op", "node.update", "id", id, "patch", patch)); hlNodes.add(id); }
        void updateEdge(String id, Map<String, Object> patch) { ops.add(Map.of("op", "edge.update", "id", id, "patch", patch)); hlEdges.add(id); }
        void moveNode(NodeModel n, double dy) { if (dy != 0) ops.add(Map.of("op", "node.update", "id", n.id(), "patch", Map.of("x", n.x(), "y", Math.round(n.y() + dy)))); }
        String name(NodeModel n) { return n.label() == null || n.label().isBlank() ? n.id() : n.label(); }
        Map<String, Object> props(NodeModel n) { if (pendingProps.containsKey(n.id())) return new LinkedHashMap<>(pendingProps.get(n.id())); return n.properties() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(n.properties()); }
    }

    private static double avgX(Collection<NodeModel> ns) { return ns.stream().mapToDouble(NodeModel::x).average().orElse(0); }

    /** Push every node at or below y down by dy to make room for a new layer. */
    private static void makeRoom(Ctx c, double y, double dy) {
        for (NodeModel n : c.nodes) if (n.y() >= y - 1) c.moveNode(n, dy);
    }

    // ---------- patterns ----------
    private void apiGateway(Ctx c) {
        List<NodeModel> clients = c.nodes.stream().filter(DesignPatternEngine::isClient).toList();
        if (clients.isEmpty()) { c.reason = "Add a client (web or mobile app) first; an API gateway sits between clients and services."; return; }
        List<EdgeModel> direct = new ArrayList<>();
        for (NodeModel cl : clients) for (EdgeModel e : c.out(cl.id())) {
            NodeModel tgt = c.node(e.targetId());
            if (tgt != null && !isGateway(tgt) && !isLb(tgt) && !isClient(tgt)) direct.add(e);
        }
        if (direct.isEmpty()) { c.reason = "Clients already go through a gateway."; return; }
        NodeModel existing = c.nodes.stream().filter(DesignPatternEngine::isGateway).findFirst().orElse(null);
        String gw;
        if (existing != null) gw = existing.id();
        else {
            double clientY = clients.stream().mapToDouble(NodeModel::y).max().orElse(0);
            makeRoom(c, clientY + 60, 140);
            gw = c.addNode("api", "API Gateway", "Single entry point: routing, authentication, rate limiting.", avgX(clients), clientY + 130,
                    Map.of("technology", "Spring Cloud Gateway", "responsibilities", List.of("Request routing", "Authentication", "Rate limiting"), "pattern", "api-gateway"));
            c.changes.add("Added API Gateway as the single entry point");
        }
        Set<String> linkedClients = new HashSet<>(); Set<String> linkedTargets = new HashSet<>();
        c.edges.stream().filter(e -> gw.equals(e.targetId())).forEach(e -> linkedClients.add(e.sourceId()));
        c.edges.stream().filter(e -> gw.equals(e.sourceId())).forEach(e -> linkedTargets.add(e.targetId()));
        for (EdgeModel e : direct) {
            c.removeEdge(e);
            if (linkedClients.add(e.sourceId())) c.addEdge(e.sourceId(), gw, "sync", "HTTPS");
            if (linkedTargets.add(e.targetId())) c.addEdge(gw, e.targetId(), "sync", "REST");
            c.changes.add("Rerouted " + c.name(c.node(e.sourceId())) + " → " + c.name(c.node(e.targetId())) + " through the gateway");
        }
    }

    private void bff(Ctx c) {
        List<NodeModel> clients = c.nodes.stream().filter(DesignPatternEngine::isClient).toList();
        if (clients.isEmpty()) { c.reason = "Add at least one client (web or mobile app) first."; return; }
        double clientY = clients.stream().mapToDouble(NodeModel::y).max().orElse(0);
        List<NodeModel> todo = clients.stream().filter(cl -> c.out(cl.id()).stream().noneMatch(e -> l(c.node(e.targetId()) == null ? cl : c.node(e.targetId())).contains("bff"))).toList();
        if (todo.isEmpty()) { c.reason = "Every client already has its own BFF."; return; }
        makeRoom(c, clientY + 60, 140);
        for (NodeModel cl : todo) {
            String bff = c.addNode("service", c.name(cl) + " BFF", "Backend for Frontend: aggregates and shapes data for " + c.name(cl) + ".",
                    cl.x(), clientY + 130, Map.of("technology", "Spring Boot", "responsibilities", List.of("Aggregate downstream calls", "Shape responses for the UI"), "pattern", "bff"));
            Set<String> targets = new LinkedHashSet<>();
            for (EdgeModel e : c.out(cl.id())) { c.removeEdge(e); targets.add(e.targetId()); }
            c.addEdge(cl.id(), bff, "sync", "HTTPS");
            for (String tg : targets) c.addEdge(bff, tg, "sync", "REST");
            c.changes.add("Added " + c.name(cl) + " BFF between the client and " + (targets.isEmpty() ? "the backend" : targets.size() + " downstream component(s)"));
        }
    }

    private void databasePerService(Ctx c) {
        boolean any = false;
        for (NodeModel db : c.nodes.stream().filter(DesignPatternEngine::isDb).toList()) {
            List<EdgeModel> incoming = c.in(db.id()).stream().filter(e -> { NodeModel s = c.node(e.sourceId()); return s != null && isService(s); }).toList();
            if (incoming.size() < 2) continue;
            any = true;
            // The first service keeps the original database; the others get their own.
            for (int i = 1; i < incoming.size(); i++) {
                EdgeModel e = incoming.get(i); NodeModel svc = c.node(e.sourceId());
                String own = c.addNode("database", c.name(svc) + " DB", "Private database owned by " + c.name(svc) + ".",
                        svc.x(), Math.max(db.y(), svc.y() + 150), Map.of("technology", "PostgreSQL", "owner", svc.id(), "pattern", "database-per-service"));
                c.removeEdge(e);
                c.addEdge(svc.id(), own, "dependency", "SQL");
                c.changes.add("Gave " + c.name(svc) + " its own database instead of sharing " + c.name(db));
            }
            NodeModel keeper = c.node(incoming.get(0).sourceId());
            Map<String, Object> p = c.props(db); p.put("owner", keeper.id()); p.put("pattern", "database-per-service");
            c.updateNode(db.id(), Map.of("label", c.name(keeper) + " DB", "properties", p));
            c.changes.add(c.name(db) + " is now owned only by " + c.name(keeper));
        }
        if (!any) c.reason = "No database is shared by two or more services.";
    }

    private void cacheAside(Ctx c) {
        for (NodeModel db : c.nodes.stream().filter(DesignPatternEngine::isDb).toList()) {
            List<EdgeModel> readers = c.in(db.id()).stream().filter(e -> { NodeModel s = c.node(e.sourceId()); return s != null && !isCache(s); }).toList();
            if (readers.isEmpty()) continue;
            boolean hasCache = readers.stream().anyMatch(e -> c.out(e.sourceId()).stream().anyMatch(o -> { NodeModel t = c.node(o.targetId()); return t != null && isCache(t); }));
            if (hasCache) continue;
            String cache = c.addNode("cache", "Redis Cache (" + c.name(db) + ")", "Cache-aside: read here first, fall back to " + c.name(db) + " on miss and populate.",
                    db.x() + 220, db.y() - 70, Map.of("technology", "Redis", "ttlSeconds", 300, "pattern", "cache-aside"));
            for (EdgeModel e : readers) c.addEdge(e.sourceId(), cache, "sync", "GET/SET");
            c.changes.add("Added a cache in front of " + c.name(db) + " for " + readers.size() + " reader(s)");
        }
        if (c.ops.isEmpty()) c.reason = "No database without a cache was found.";
    }

    private void cqrs(Ctx c) {
        for (NodeModel svc : c.nodes.stream().filter(DesignPatternEngine::isService).toList()) {
            List<NodeModel> dbs = c.out(svc.id()).stream().map(e -> c.node(e.targetId())).filter(n -> n != null && isDb(n)).toList();
            if (dbs.isEmpty()) continue;
            if (c.out(svc.id()).stream().anyMatch(e -> { NodeModel t = c.node(e.targetId()); return t != null && l(t).contains("read model"); })) continue;
            NodeModel write = dbs.get(0);
            Map<String, Object> p = c.props(write); p.put("role", "write-model"); p.put("pattern", "cqrs");
            c.updateNode(write.id(), Map.of("properties", p));
            String read = c.addNode("database", c.name(svc) + " Read Model", "Denormalised read store for queries, updated from write-side events.",
                    write.x() + 220, write.y(), Map.of("technology", "Elasticsearch", "role", "read-model", "pattern", "cqrs"));
            c.addEdge(svc.id(), read, "sync", "Queries");
            c.addEdge(write.id(), read, "event", "Projection");
            c.out(svc.id()).stream().filter(e -> e.targetId().equals(write.id())).forEach(e -> c.updateEdge(e.id(), Map.of("label", "Commands")));
            c.changes.add("Split " + c.name(svc) + " into a write model (" + c.name(write) + ") and a read model");
        }
        if (c.ops.isEmpty()) c.reason = "CQRS needs a service that owns a database.";
    }

    private void eventDriven(Ctx c) {
        List<EdgeModel> svcToSvc = c.edges.stream().filter(e -> isSync(e)).filter(e -> {
            NodeModel s = c.node(e.sourceId()), t = c.node(e.targetId());
            return s != null && t != null && isService(s) && isService(t);
        }).toList();
        if (svcToSvc.isEmpty()) { c.reason = "No synchronous service-to-service calls to convert into events."; return; }
        NodeModel existing = c.nodes.stream().filter(DesignPatternEngine::isQueue).findFirst().orElse(null);
        String broker;
        if (existing != null) broker = existing.id();
        else {
            Set<NodeModel> involved = new LinkedHashSet<>();
            svcToSvc.forEach(e -> { involved.add(c.node(e.sourceId())); involved.add(c.node(e.targetId())); });
            double maxX = c.nodes.stream().mapToDouble(NodeModel::x).max().orElse(0);
            double y = involved.stream().mapToDouble(NodeModel::y).average().orElse(0);
            broker = c.addNode("queue", "Event Broker", "Publish/subscribe broker; services emit domain events instead of calling each other.",
                    maxX + 260, y, Map.of("technology", "Kafka", "pattern", "event-driven"));
            c.changes.add("Added an Event Broker");
        }
        Set<String> pubs = new HashSet<>(), subs = new HashSet<>();
        c.out(broker).forEach(e -> subs.add(e.targetId())); c.in(broker).forEach(e -> pubs.add(e.sourceId()));
        for (EdgeModel e : svcToSvc) {
            c.removeEdge(e);
            if (pubs.add(e.sourceId())) c.addEdge(e.sourceId(), broker, "event", "publish");
            if (subs.add(e.targetId())) c.addEdge(broker, e.targetId(), "event", "subscribe");
            c.changes.add("Replaced " + c.name(c.node(e.sourceId())) + " → " + c.name(c.node(e.targetId())) + " call with an event");
        }
    }

    private void saga(Ctx c) {
        NodeModel driver = null; List<EdgeModel> calls = List.of();
        for (NodeModel s : c.nodes.stream().filter(DesignPatternEngine::isService).toList()) {
            List<EdgeModel> o = c.out(s.id()).stream().filter(e -> { NodeModel t = c.node(e.targetId()); return t != null && isService(t); }).toList();
            if (o.size() > calls.size()) { driver = s; calls = o; }
        }
        if (driver == null || calls.size() < 2) { c.reason = "A saga needs one service that coordinates at least two other services."; return; }
        String orch = c.addNode("service", c.name(driver) + " Saga Orchestrator",
                "Runs the multi-step transaction started by " + c.name(driver) + " and triggers compensations on failure.",
                driver.x() + 240, driver.y(), Map.of("technology", "Spring Boot + state machine", "responsibilities", List.of("Coordinate steps", "Compensate on failure", "Track saga state"), "pattern", "saga"));
        c.addEdge(driver.id(), orch, "async", "start saga");
        for (EdgeModel e : calls) {
            c.removeEdge(e);
            c.addEdge(orch, e.targetId(), "async", "command / compensate");
        }
        c.changes.add("Added a saga orchestrator for " + c.name(driver));
        c.changes.add("Moved " + calls.size() + " direct calls from " + c.name(driver) + " into saga steps with compensation");
    }

    private void circuitBreaker(Ctx c) {
        for (EdgeModel e : c.edges) {
            NodeModel s = c.node(e.sourceId()), t = c.node(e.targetId());
            if (s == null || t == null || !isSync(e)) continue;
            if (!(isService(s) || isGateway(s))) continue;
            if (!(isService(t) || isExternal(t))) continue;
            if (e.properties() != null && "circuit-breaker".equals(e.properties().get("resilience"))) continue;
            Map<String, Object> p = e.properties() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(e.properties());
            p.put("resilience", "circuit-breaker"); p.put("timeoutMs", 2000); p.put("retries", 2); p.put("fallback", "cached/default response");
            String label = (e.label() == null || e.label().isBlank() ? "REST" : e.label()) + " ⚡CB";
            c.updateEdge(e.id(), Map.of("label", label, "properties", p));
            Map<String, Object> np = c.props(s); np.put("resilience", "Resilience4j");
            c.updateNode(s.id(), Map.of("properties", np));
            c.changes.add("Wrapped " + c.name(s) + " → " + c.name(t) + " in a circuit breaker (2s timeout, 2 retries, fallback)");
        }
        if (c.ops.isEmpty()) c.reason = "No unprotected synchronous calls between services.";
    }

    private void loadBalancer(Ctx c) {
        NodeModel entry = c.nodes.stream().filter(DesignPatternEngine::isGateway).findFirst()
                .orElse(c.nodes.stream().filter(DesignPatternEngine::isService).findFirst().orElse(null));
        if (entry == null) { c.reason = "Add a gateway or service first."; return; }
        boolean hasLb = c.nodes.stream().anyMatch(DesignPatternEngine::isLb);
        if (!hasLb) {
            List<EdgeModel> into = c.in(entry.id());
            makeRoom(c, entry.y(), 130);
            String lb = c.addNode("api", "Load Balancer", "Distributes traffic across healthy replicas; health checks and TLS termination.",
                    entry.x(), entry.y(), Map.of("technology", "NGINX / AWS ALB", "pattern", "load-balancer"));
            for (EdgeModel e : into) { c.removeEdge(e); c.addEdge(e.sourceId(), lb, e.type() == null ? "sync" : e.type(), e.label()); }
            c.addEdge(lb, entry.id(), "sync", "round-robin");
            c.changes.add("Added a Load Balancer in front of " + c.name(entry));
        }
        for (NodeModel n : c.nodes) {
            if (!(isService(n) || isGateway(n))) continue;
            Object r = n.properties() == null ? null : n.properties().get("replicas");
            if (r instanceof Number num && num.intValue() >= 2) continue;
            Map<String, Object> p = c.props(n); p.put("replicas", 3); p.put("stateless", true);
            c.updateNode(n.id(), Map.of("properties", p));
            c.changes.add("Scaled " + c.name(n) + " to 3 stateless replicas");
        }
        if (c.ops.isEmpty()) c.reason = "Already load-balanced with replicas.";
    }

    // ---------- SOLID principles ----------
    static String kind(NodeModel n) {
        if (isDb(n)) return "Data"; if (isCache(n)) return "Data"; if (isQueue(n)) return "Messaging";
        if (isExternal(n)) return "Integration"; if (isService(n)) return "Orchestration"; return "Other";
    }
    static boolean isAbstraction(NodeModel n) {
        String l = l(n), t = t(n);
        return l.contains("repository") || l.contains("adapter") || l.contains("port") || l.contains("strategy") || l.contains("interface")
                || t.contains("adapter") || "solid-dip".equals(String.valueOf(n.properties() == null ? null : n.properties().get("pattern")))
                || "solid-ocp".equals(String.valueOf(n.properties() == null ? null : n.properties().get("pattern")));
    }

    /** S: split a service whose outgoing dependencies span several responsibility kinds. */
    private void srp(Ctx c) {
        boolean any = false;
        for (NodeModel svc : c.nodes.stream().filter(DesignPatternEngine::isService).toList()) {
            List<EdgeModel> outs = c.out(svc.id()).stream().filter(e -> c.node(e.targetId()) != null && !isAbstraction(c.node(e.targetId()))).toList();
            Map<String, List<EdgeModel>> byKind = new LinkedHashMap<>();
            for (EdgeModel e : outs) byKind.computeIfAbsent(kind(c.node(e.targetId())), k -> new ArrayList<>()).add(e);
            byKind.remove("Other");
            if (byKind.size() < 3 && outs.size() < 5) continue;
            if (byKind.size() < 2) continue;
            any = true;
            String keep = byKind.containsKey("Data") ? "Data" : byKind.keySet().iterator().next();
            int i = 0;
            for (var entry : byKind.entrySet()) {
                if (entry.getKey().equals(keep)) continue;
                String label = c.name(svc) + " " + entry.getKey();
                String split = c.addNode("service", label, "Extracted from " + c.name(svc) + " (Single Responsibility): owns only " + entry.getKey().toLowerCase() + " concerns.",
                        svc.x() + 230 * (++i), svc.y() + 170, Map.of("pattern", "solid-srp", "extractedFrom", svc.id(), "responsibility", entry.getKey()));
                for (EdgeModel e : entry.getValue()) { c.removeEdge(e); c.addEdge(split, e.targetId(), e.type() == null ? "sync" : e.type(), e.label()); }
                c.addEdge(svc.id(), split, "sync", "delegates");
                c.changes.add("Moved " + entry.getKey().toLowerCase() + " work out of " + c.name(svc) + " into " + label);
            }
            Map<String, Object> p = c.props(svc); p.put("responsibility", keep); p.put("pattern", "solid-srp");
            c.updateNode(svc.id(), Map.of("properties", p));
        }
        if (!any && c.reason == null) c.reason = "No service mixes three or more kinds of responsibility.";
    }

    /** O: route interchangeable targets of the same type through a strategy/extension point. */
    private void ocp(Ctx c) {
        boolean any = false;
        for (NodeModel svc : c.nodes.stream().filter(DesignPatternEngine::isService).toList()) {
            Map<String, List<EdgeModel>> byType = new LinkedHashMap<>();
            for (EdgeModel e : c.out(svc.id())) { NodeModel t = c.node(e.targetId()); if (t != null && !isAbstraction(t) && (isExternal(t) || isQueue(t) || isDb(t))) byType.computeIfAbsent(t(t), k -> new ArrayList<>()).add(e); }
            for (var entry : byType.entrySet()) {
                if (entry.getValue().size() < 2) continue;
                any = true;
                String typeName = entry.getKey().isBlank() ? "Provider" : Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1);
                String strategy = c.addNode("service", c.name(svc) + " " + typeName + " Strategy",
                        "Open/Closed extension point: new " + entry.getKey() + " implementations are registered here without changing " + c.name(svc) + ".",
                        svc.x() + 240, svc.y() - 120, Map.of("pattern", "solid-ocp", "implementations", entry.getValue().size()));
                for (EdgeModel e : entry.getValue()) { c.removeEdge(e); c.addEdge(strategy, e.targetId(), e.type() == null ? "sync" : e.type(), "implementation"); }
                c.addEdge(svc.id(), strategy, "sync", "uses interface");
                c.changes.add(c.name(svc) + " now depends on one " + typeName + " Strategy instead of " + entry.getValue().size() + " hard-wired " + entry.getKey() + "s");
            }
        }
        if (!any && c.reason == null) c.reason = "No service hard-wires two or more interchangeable components.";
    }

    /** L: give interchangeable components of the same type one shared contract. */
    private void lsp(Ctx c) {
        Map<String, List<NodeModel>> sameRole = new LinkedHashMap<>();
        for (NodeModel src : c.nodes) {
            Map<String, List<NodeModel>> byType = new LinkedHashMap<>();
            for (EdgeModel e : c.out(src.id())) { NodeModel t = c.node(e.targetId()); if (t != null && !isClient(t)) byType.computeIfAbsent(t(t), k -> new ArrayList<>()).add(t); }
            byType.forEach((type, list) -> { if (list.size() >= 2) sameRole.computeIfAbsent(type, k -> new ArrayList<>()).addAll(list); });
        }
        for (var entry : sameRole.entrySet()) {
            String contract = (entry.getKey().isBlank() ? "Component" : Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1)) + "Contract";
            Set<String> done = new HashSet<>();
            for (NodeModel n : entry.getValue()) {
                if (!done.add(n.id()) || contract.equals(c.props(n).get("contract"))) continue;
                Map<String, Object> p = c.props(n); p.put("contract", contract); p.put("pattern", "solid-lsp");
                p.put("lspRules", "same inputs accepted, same outputs/errors guaranteed, no stronger preconditions");
                c.updateNode(n.id(), Map.of("properties", p));
                c.changes.add(c.name(n) + " now implements " + contract + " and can substitute its siblings");
            }
        }
        if (c.changes.stream().noneMatch(x -> x.contains("Contract")) && c.reason == null) c.reason = "No interchangeable components are used in the same role.";
    }

    /** I: split a widely-used service interface into consumer-specific interfaces. */
    private void isp(Ctx c) {
        boolean any = false;
        for (NodeModel svc : c.nodes.stream().filter(n -> isService(n) || isGateway(n)).toList()) {
            List<EdgeModel> in = c.in(svc.id()).stream().filter(e -> c.node(e.sourceId()) != null).toList();
            Set<String> callers = new LinkedHashSet<>(); in.forEach(e -> callers.add(e.sourceId()));
            if (callers.size() < 3 || c.props(svc).containsKey("interfaces")) continue;
            any = true;
            List<String> ifaces = new ArrayList<>();
            for (EdgeModel e : in) {
                String iface = "I" + c.name(svc).replaceAll("[^A-Za-z0-9]", "") + "For" + c.name(c.node(e.sourceId())).replaceAll("[^A-Za-z0-9]", "");
                if (!ifaces.contains(iface)) ifaces.add(iface);
                Map<String, Object> ep = e.properties() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(e.properties()); ep.put("interface", iface);
                c.updateEdge(e.id(), Map.of("label", iface, "properties", ep));
            }
            Map<String, Object> p = c.props(svc); p.put("interfaces", ifaces); p.put("pattern", "solid-isp");
            c.updateNode(svc.id(), Map.of("properties", p));
            c.changes.add("Split " + c.name(svc) + "'s API into " + ifaces.size() + " consumer-specific interfaces");
        }
        if (!any && c.reason == null) c.reason = "No service is shared by three or more different consumers.";
    }

    /** D: insert a repository port / adapter between a service and concrete infrastructure. */
    private void dip(Ctx c) {
        boolean any = false;
        for (NodeModel svc : c.nodes.stream().filter(DesignPatternEngine::isService).toList()) {
            if (isAbstraction(svc)) continue;
            for (EdgeModel e : c.out(svc.id())) {
                NodeModel t = c.node(e.targetId());
                if (t == null || isAbstraction(t) || !(isDb(t) || isCache(t) || isQueue(t) || isExternal(t))) continue;
                any = true;
                String suffix = isDb(t) ? " Repository" : isQueue(t) ? " Publisher Port" : isCache(t) ? " Cache Port" : " Adapter";
                String label = (isExternal(t) ? c.name(t) : c.name(svc)) + suffix;
                String port = c.addNode("service", label, "Dependency Inversion: " + c.name(svc) + " depends on this abstraction; the concrete " + c.name(t) + " is an implementation detail.",
                        (svc.x() + t.x()) / 2, (svc.y() + t.y()) / 2 + 40, Map.of("pattern", "solid-dip", "abstracts", t.id(), "layer", "adapter"));
                c.removeEdge(e);
                c.addEdge(svc.id(), port, "sync", "interface");
                c.addEdge(port, t.id(), e.type() == null ? "sync" : e.type(), e.label());
                c.changes.add(c.name(svc) + " now depends on " + label + " instead of " + c.name(t) + " directly");
            }
        }
        if (!any && c.reason == null) c.reason = "No service depends directly on concrete infrastructure.";
    }
}
