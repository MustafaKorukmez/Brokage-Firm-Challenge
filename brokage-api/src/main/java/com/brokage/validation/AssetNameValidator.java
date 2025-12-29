package com.brokage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class AssetNameValidator implements ConstraintValidator<ValidAssetName, String> {

    private static final Pattern ASSET_NAME_PATTERN = Pattern.compile("^[A-Z]{2,10}$");

    @Override
    public void initialize(ValidAssetName constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return ASSET_NAME_PATTERN.matcher(value).matches();
    }
}
