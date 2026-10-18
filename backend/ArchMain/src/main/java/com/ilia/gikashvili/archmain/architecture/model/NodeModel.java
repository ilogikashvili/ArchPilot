package com.ilia.gikashvili.archmain.architecture.model;

import java.util.Map;

/**
 * Typed model for a node/component in the architecture diagram.
 */
public record NodeModel(
        String id,
        String type,
        String label,
        String description,
        double x,
        double y,
        Map<String, Object> properties
) {
}
