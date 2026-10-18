package com.ilia.gikashvili.archmain.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource is not found.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public NotFoundException(String resourceType, Object id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND",
                resourceType + " with id '" + id + "' not found");
    }
}
