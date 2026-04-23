package com.Reffr_Backend.common.exception;

import org.springframework.http.HttpStatus;

/** 400 — business rule violation. */
public class BusinessException extends BaseException {

    public BusinessException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
