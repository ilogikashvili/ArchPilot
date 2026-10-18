package com.ilia.gikashvili.archmain.architecture.validation.rules;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.model.EdgeModel;
import com.ilia.gikashvili.archmain.architecture.model.NodeModel;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EdgeReferencesExistRule implements ValidationRule {

    @Override
    public void validate(ArchitectureDocument document, ValidationResult result) {
        Set<String> nodeIds = document.nodes().stream()
                .map(NodeModel::id)
                .collect(Collectors.toSet());

        for (EdgeModel edge : document.edges()) {
            if (edge.sourceId() == null || !nodeIds.contains(edge.sourceId())) {
                result.addError("EdgeReferencesExist",
                        "Edge '" + edge.id() + "' references non-existent source node: " + edge.sourceId(),
                        edge.id());
            }
            if (edge.targetId() == null || !nodeIds.contains(edge.targetId())) {
                result.addError("EdgeReferencesExist",
                        "Edge '" + edge.id() + "' references non-existent target node: " + edge.targetId(),
                        edge.id());
            }
        }
    }
}
