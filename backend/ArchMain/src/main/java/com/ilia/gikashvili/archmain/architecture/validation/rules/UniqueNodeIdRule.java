package com.ilia.gikashvili.archmain.architecture.validation.rules;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.model.NodeModel;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UniqueNodeIdRule implements ValidationRule {

    @Override
    public void validate(ArchitectureDocument document, ValidationResult result) {
        Set<String> seen = new HashSet<>();
        for (NodeModel node : document.nodes()) {
            if (node.id() == null || node.id().isBlank()) {
                result.addError("UniqueNodeId", "Node has a null or blank id");
            } else if (!seen.add(node.id())) {
                result.addError("UniqueNodeId", "Duplicate node id: " + node.id(), node.id());
            }
        }
    }
}
