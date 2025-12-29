package com.brokage.exception;

import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends BusinessException {

    private static final String ERROR_CODE = "INVALID_ORDER_STATE";

    public InvalidOrderStateException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }
}
