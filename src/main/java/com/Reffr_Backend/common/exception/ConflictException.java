package com.Reffr_Backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 409 Conflict - Resource already exists or state conflict.
 */
public class ConflictException extends BaseException {
    public ConflictException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}
