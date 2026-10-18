package com.ilia.gikashvili.archmain.architecture.validation.rules;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.model.EdgeModel;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UniqueEdgeIdRule implements ValidationRule {

    @Override
    public void validate(ArchitectureDocument document, ValidationResult result) {
        Set<String> seen = new HashSet<>();
        for (EdgeModel edge : document.edges()) {
            if (edge.id() == null || edge.id().isBlank()) {
                result.addError("UniqueEdgeId", "Edge has a null or blank id");
            } else if (!seen.add(edge.id())) {
                result.addError("UniqueEdgeId", "Duplicate edge id: " + edge.id(), edge.id());
            }
        }
    }
}
