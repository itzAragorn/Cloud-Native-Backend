package com.bancocloud.rabbit_admin_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;

public class NoReservadoValidator implements ConstraintValidator<NoReservado, String> {

    private static final String PREFIJO_RESERVADO = "amq.";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || !value.toLowerCase(Locale.ROOT).startsWith(PREFIJO_RESERVADO);
    }
}
