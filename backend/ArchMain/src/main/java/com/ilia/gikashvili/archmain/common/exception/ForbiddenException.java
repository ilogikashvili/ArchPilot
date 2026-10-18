package com.ilia.gikashvili.archmain.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the current user lacks permission for an operation.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action");
    }
}
