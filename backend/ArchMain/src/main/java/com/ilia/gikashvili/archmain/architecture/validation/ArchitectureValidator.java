package com.ilia.gikashvili.archmain.architecture.validation;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.validation.rules.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs all registered validation rules against a document.
 */
@Component
public class ArchitectureValidator {

    private final List<ValidationRule> rules;

    public ArchitectureValidator(List<ValidationRule> rules) {
        this.rules = rules;
    }

    /**
     * Validate the document against all rules.
     */
    public ValidationResult validate(ArchitectureDocument document) {
        ValidationResult result = new ValidationResult();
        for (ValidationRule rule : rules) {
            rule.validate(document, result);
        }
        return result;
    }
}
