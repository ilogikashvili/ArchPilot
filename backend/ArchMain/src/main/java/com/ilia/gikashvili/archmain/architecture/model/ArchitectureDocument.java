package com.ilia.gikashvili.archmain.architecture.model;

import java.util.List;
import java.util.Map;

/**
 * The complete architecture document.
 * This is the typed shape of the JSONB stored in the database.
 */
public record ArchitectureDocument(
        String schemaVersion,
        List<NodeModel> nodes,
        List<EdgeModel> edges,
        Viewport viewport,
        Map<String, Object> metadata
) {
    /**
     * Create an empty architecture document with the current schema version.
     */
    public static ArchitectureDocument empty() {
        return new ArchitectureDocument("1.0", List.of(), List.of(), new Viewport(0, 0, 1.0), Map.of());
    }
}
