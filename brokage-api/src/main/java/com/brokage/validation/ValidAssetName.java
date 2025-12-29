package com.brokage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AssetNameValidator.class)
public @interface ValidAssetName {

    String message() default "Invalid asset name. Must be 2-10 uppercase letters (e.g., AAPL, GOOG)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
