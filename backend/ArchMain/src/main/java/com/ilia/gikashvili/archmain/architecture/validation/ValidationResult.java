package com.ilia.gikashvili.archmain.architecture.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects validation errors from all rules.
 */
public class ValidationResult {

    private final List<ValidationError> errors = new ArrayList<>();

    public void addError(String rule, String message) {
        errors.add(new ValidationError(rule, message, null));
    }

    public void addError(String rule, String message, String elementId) {
        errors.add(new ValidationError(rule, message, elementId));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<ValidationError> getErrors() {
        return List.copyOf(errors);
    }

    public record ValidationError(
            String rule,
            String message,
            String elementId
    ) {
    }
}
