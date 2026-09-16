package com.bancocloud.usuarios_service.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotación personalizada para validar contraseñas
 * Requisitos:
 * - Mínimo 8 caracteres
 * - Al menos 1 mayúscula
 * - Al menos 1 número
 * - Al menos 1 símbolo especial
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
    String message() default "La contraseña no cumple los requisitos: mínimo 8 caracteres, 1 mayúscula, 1 número y 1 símbolo especial";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
