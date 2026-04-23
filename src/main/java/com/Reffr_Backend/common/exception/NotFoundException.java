package com.Reffr_Backend.common.exception;

import org.springframework.http.HttpStatus;

/** 404 — resource could not be found. */
public class NotFoundException extends BaseException {

    public NotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}
