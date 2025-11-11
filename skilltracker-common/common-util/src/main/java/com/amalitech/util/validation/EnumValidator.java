package com.amalitech.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for enum values.
 * <p>
 * Note: This validator returns {@code true} for {@code null} values, following the
 * Bean Validation (JSR 380) best practice that custom validators should not validate
 * null values. Use {@code @NotNull} in combination with {@code @ValidEnum} when null
 * values should be rejected.
 * </p>
 */
public class EnumValidator implements ConstraintValidator<ValidEnum, Enum<?>> {
    private Class<? extends Enum<?>> enumClass;

    @Override
    public void initialize(ValidEnum annotation) {
        this.enumClass = annotation.enumClass();
    }

    @Override
    public boolean isValid(Enum<?> value, ConstraintValidatorContext context) {
        // Return true for null values - null checking should be done via @NotNull annotation
        if (value == null) {
            return true;
        }

        Enum<?>[] enumConstants = enumClass.getEnumConstants();
        for (Enum<?> enumConstant : enumConstants) {
            if (enumConstant.equals(value)) {
                return true;
            }
        }
        return false;
    }
}