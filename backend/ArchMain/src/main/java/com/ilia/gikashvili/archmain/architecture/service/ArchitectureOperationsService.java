package com.ilia.gikashvili.archmain.architecture.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.architecture.entity.ArchitectureDocumentEntity;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.repository.ArchitectureDocumentRepository;
import com.ilia.gikashvili.archmain.architecture.validation.ArchitectureValidator;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import com.ilia.gikashvili.archmain.common.exception.ValidationException;
import com.ilia.gikashvili.archmain.realtime.OperationsApplied;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies fine-grained edit operations to the latest architecture document.
 *
 * Unlike PUT /architecture (whole-document, optimistic revision check), operations never conflict:
 * each one targets a single node/edge by id and is applied to whatever the current document is.
 * This is what lets many people and AI agents edit the same diagram at the same time.
 * Operations for one project are serialized in-process; every accepted batch bumps the revision
 * and is broadcast to all live clients as "operations.applied".
 */
@Service
public class ArchitectureOperationsService {
    private static final Set<String> NODE_FIELDS = Set.of("type", "label", "description", "x", "y", "properties");
    private static final Set<String> EDGE_FIELDS = Set.of("type", "label", "sourceId", "targetId", "properties");

    private final ArchitectureDocumentRepository repository;
    private final ArchitectureMapper mapper;
    private final ArchitectureValidator validator;
    private final ProjectAccessService access;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;
    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();

    public ArchitectureOperationsService(ArchitectureDocumentRepository repository, ArchitectureMapper mapper,
                                         ArchitectureValidator validator, ProjectAccessService access,
                                         ApplicationEventPublisher events, ObjectMapper json) {
        this.repository = repository; this.mapper = mapper; this.validator = validator;
        this.access = access; this.events = events; this.json = json;
    }

    public Object lockFor(UUID projectId) { return locks.computeIfAbsent(projectId, k -> new Object()); }

    @Transactional
    public ArchitectureDocumentEntity apply(UUID projectId, UUID userId, String clientId, List<Map<String, Object>> ops) {
        access.require(projectId, userId, Permission.EDIT);
        if (ops == null || ops.isEmpty()) throw new ValidationException("At least one operation is required");
        if (ops.size() > 500) throw new ValidationException("At most 500 operations per request");

        ArchitectureDocumentEntity entity = repository.findByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException("Architecture document for project", projectId));
        Map<String, Object> doc = json.convertValue(mapper.toModel(entity), new TypeReference<>() {});
        List<Map<String, Object>> nodes = mutableList(doc.get("nodes"));
        List<Map<String, Object>> edges = mutableList(doc.get("edges"));

        for (int i = 0; i < ops.size(); i++) {
            applyOne(ops.get(i), i, nodes, edges);
        }
        doc.put("nodes", nodes);
        doc.put("edges", edges);

        ArchitectureDocument updated = json.convertValue(doc, ArchitectureDocument.class);
        var result = validator.validate(updated);
        if (!result.isValid()) throw new ValidationException("Operations produce an invalid document: " + result.getErrors().get(0).message());

        entity.setDocumentJson(mapper.toJson(updated));
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(Instant.now());
        var saved = repository.saveAndFlush(entity);
        events.publishEvent(new OperationsApplied(projectId, userId, clientId, saved.getRevision(), ops));
        return saved;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mutableList(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) out.add(new LinkedHashMap<>((Map<String, Object>) x));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o, String what, int index) {
        if (!(o instanceof Map<?, ?> m)) throw new ValidationException("Operation " + index + ": '" + what + "' must be an object");
        return new LinkedHashMap<>((Map<String, Object>) m);
    }

    private static String id(Map<String, Object> op, int index) {
        Object id = op.get("id");
        if (!(id instanceof String s) || s.isBlank()) throw new ValidationException("Operation " + index + ": 'id' is required");
        return s;
    }

    private static Optional<Map<String, Object>> find(List<Map<String, Object>> list, String id) {
        return list.stream().filter(x -> id.equals(x.get("id"))).findFirst();
    }

    private void applyOne(Map<String, Object> op, int i, List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        String kind = String.valueOf(op.get("op"));
        switch (kind) {
            case "node.add" -> {
                Map<String, Object> n = asMap(op.get("node"), "node", i);
                String nid = id(n, i);
                if (find(nodes, nid).isPresent()) throw new ValidationException("Operation " + i + ": node '" + nid + "' already exists");
                n.putIfAbsent("description", "");
                n.putIfAbsent("properties", new LinkedHashMap<>());
                n.putIfAbsent("x", 0); n.putIfAbsent("y", 0);
                nodes.add(n);
            }
            case "node.update" -> {
                String nid = id(op, i);
                // Updates to nodes deleted concurrently by someone else are ignored rather than failing the batch.
                find(nodes, nid).ifPresent(n -> asMap(op.get("patch"), "patch", i).forEach((k, v) -> { if (NODE_FIELDS.contains(k)) n.put(k, v); }));
            }
            case "node.remove" -> {
                String nid = id(op, i);
                nodes.removeIf(n -> nid.equals(n.get("id")));
                edges.removeIf(e -> nid.equals(e.get("sourceId")) || nid.equals(e.get("targetId")));
            }
            case "edge.add" -> {
                Map<String, Object> e = asMap(op.get("edge"), "edge", i);
                String eid = id(e, i);
                if (find(edges, eid).isPresent()) throw new ValidationException("Operation " + i + ": edge '" + eid + "' already exists");
                if (find(nodes, String.valueOf(e.get("sourceId"))).isEmpty() || find(nodes, String.valueOf(e.get("targetId"))).isEmpty())
                    throw new ValidationException("Operation " + i + ": edge endpoints must exist");
                e.putIfAbsent("properties", new LinkedHashMap<>());
                e.putIfAbsent("type", "sync");
                edges.add(e);
            }
            case "edge.update" -> {
                String eid = id(op, i);
                find(edges, eid).ifPresent(e -> asMap(op.get("patch"), "patch", i).forEach((k, v) -> { if (EDGE_FIELDS.contains(k)) e.put(k, v); }));
            }
            case "edge.remove" -> {
                String eid = id(op, i);
                edges.removeIf(e -> eid.equals(e.get("id")));
            }
            default -> throw new ValidationException("Operation " + i + ": unknown op '" + kind + "'");
        }
    }
}
