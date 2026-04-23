package com.Reffr_Backend.common.exception;

import org.springframework.http.HttpStatus;

/** 401 — token missing, expired or invalid. */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String code, String message) {
        super(code, message, HttpStatus.UNAUTHORIZED);
    }
}
