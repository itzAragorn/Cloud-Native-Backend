package com.bancocloud.rabbit_admin_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nombre válido para una cola o exchange: obligatorio, máximo 100 caracteres,
 * solo letras, números, punto, guion y guion bajo.
 * <p>
 * Los nombres reservados ({@code amq.*}) se rechazan con {@link NoReservado}.
 */
@Documented
@NotBlank(message = "es obligatorio")
@Size(max = 100, message = "debe tener como máximo 100 caracteres")
@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "solo admite letras, números, '.', '_' y '-'")
@NoReservado
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NombreRecurso {

    String message() default "nombre inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
