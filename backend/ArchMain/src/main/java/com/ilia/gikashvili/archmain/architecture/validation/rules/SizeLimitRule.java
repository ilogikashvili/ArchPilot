package com.ilia.gikashvili.archmain.architecture.validation.rules;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SizeLimitRule implements ValidationRule {

    private final int maxNodes;
    private final int maxEdges;

    public SizeLimitRule(
            @Value("${archmain.validation.max-nodes:500}") int maxNodes,
            @Value("${archmain.validation.max-edges:2000}") int maxEdges) {
        this.maxNodes = maxNodes;
        this.maxEdges = maxEdges;
    }

    @Override
    public void validate(ArchitectureDocument document, ValidationResult result) {
        if (document.nodes().size() > maxNodes) {
            result.addError("SizeLimit",
                    "Document has " + document.nodes().size() + " nodes, maximum is " + maxNodes);
        }
        if (document.edges().size() > maxEdges) {
            result.addError("SizeLimit",
                    "Document has " + document.edges().size() + " edges, maximum is " + maxEdges);
        }
    }
}
