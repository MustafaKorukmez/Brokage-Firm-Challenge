package com.brokage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OrderSizeValidator.class)
public @interface ValidOrderSize {

    String message() default "Order size must be between 0.0001 and 1,000,000";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    double min() default 0.0001;

    double max() default 1000000;
}
