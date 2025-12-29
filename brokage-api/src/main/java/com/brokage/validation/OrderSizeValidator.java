package com.brokage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class OrderSizeValidator implements ConstraintValidator<ValidOrderSize, BigDecimal> {

    private BigDecimal min;
    private BigDecimal max;

    @Override
    public void initialize(ValidOrderSize constraintAnnotation) {
        this.min = BigDecimal.valueOf(constraintAnnotation.min());
        this.max = BigDecimal.valueOf(constraintAnnotation.max());
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
