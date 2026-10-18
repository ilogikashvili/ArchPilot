package com.ilia.gikashvili.archmain.architecture.model;

import java.util.Map;

/**
 * Typed model for an edge/connection in the architecture diagram.
 */
public record EdgeModel(
        String id,
        String type,
        String sourceId,
        String targetId,
        String label,
        Map<String, Object> properties
) {
}
