package com.brokage.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends BusinessException {

    private static final String ERROR_CODE = "INSUFFICIENT_BALANCE";

    public InsufficientBalanceException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public InsufficientBalanceException(String assetName, java.math.BigDecimal required,
            java.math.BigDecimal available) {
        super(ERROR_CODE,
                String.format("Insufficient %s balance. Required: %s, Available: %s", assetName, required, available),
                HttpStatus.BAD_REQUEST);
    }
}
