package com.ilia.gikashvili.archmain.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation conflicts with the current state (e.g. duplicate email).
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
