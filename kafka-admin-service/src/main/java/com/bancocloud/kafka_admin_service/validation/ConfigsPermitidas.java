package com.bancocloud.kafka_admin_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Las claves deben estar en la lista blanca de {@link ConfigsTopico} y sus valores ser válidos.
 */
@Documented
@Constraint(validatedBy = ConfigsPermitidasValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigsPermitidas {

    String message() default "configuración inválida";

    /** Al modificar, un valor nulo significa volver al valor por defecto del clúster. */
    boolean permitirNulos() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
