package com.brokage.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "ORDER_NOT_FOUND";

    public OrderNotFoundException(Long orderId) {
        super(ERROR_CODE, "Order not found with id: " + orderId, HttpStatus.NOT_FOUND);
    }

    public OrderNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }
}
