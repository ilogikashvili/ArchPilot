package com.ilia.gikashvili.archmain.architecture.validation.rules;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;

/**
 * A single validation rule for an architecture document.
 * Implement this interface and register as a Spring bean to add a new check.
 */
public interface ValidationRule {

    /**
     * Validate the document and add errors to the result.
     */
    void validate(ArchitectureDocument document, ValidationResult result);
}
