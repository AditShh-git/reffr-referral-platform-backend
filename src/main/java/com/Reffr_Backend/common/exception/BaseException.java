package com.Reffr_Backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Root of the application exception hierarchy.
 * Every domain exception carries a machine-readable code, a human message,
 * and the HTTP status to return — so the GlobalExceptionHandler needs
 * exactly one handler for all of them.
 */
public abstract class BaseException extends RuntimeException {

    private final String     code;
    private final HttpStatus status;

    protected BaseException(String code, String message, HttpStatus status) {
        super(message);
        this.code   = code;
        this.status = status;
    }

    public String     getCode()   { return code;   }
    public HttpStatus getStatus() { return status; }
}
