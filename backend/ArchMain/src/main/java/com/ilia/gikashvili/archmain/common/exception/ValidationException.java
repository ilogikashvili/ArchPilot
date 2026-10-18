package com.ilia.gikashvili.archmain.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown for domain-level validation failures (beyond @Valid annotation checks).
 */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message);
    }
}
