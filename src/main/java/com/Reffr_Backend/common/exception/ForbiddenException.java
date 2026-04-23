package com.Reffr_Backend.common.exception;

import org.springframework.http.HttpStatus;

/** 403 — authenticated but not authorised for the resource. */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }
}
