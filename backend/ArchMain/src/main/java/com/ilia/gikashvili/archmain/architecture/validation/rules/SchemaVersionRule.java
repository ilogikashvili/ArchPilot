package com.ilia.gikashvili.archmain.architecture.validation.rules;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SchemaVersionRule implements ValidationRule {

    private static final Set<String> SUPPORTED_VERSIONS = Set.of("1.0");

    @Override
    public void validate(ArchitectureDocument document, ValidationResult result) {
        if (document.schemaVersion() == null || document.schemaVersion().isBlank()) {
            result.addError("SchemaVersion", "Schema version is required");
        } else if (!SUPPORTED_VERSIONS.contains(document.schemaVersion())) {
            result.addError("SchemaVersion",
                    "Unsupported schema version: " + document.schemaVersion()
                    + ". Supported: " + SUPPORTED_VERSIONS);
        }
    }
}
