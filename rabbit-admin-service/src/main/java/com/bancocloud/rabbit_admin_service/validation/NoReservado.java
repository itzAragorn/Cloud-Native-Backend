package com.bancocloud.rabbit_admin_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rechaza nombres con el prefijo {@code amq.}, reservado por RabbitMQ. */
@Documented
@Constraint(validatedBy = NoReservadoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoReservado {

    String message() default "no puede empezar con 'amq.' (prefijo reservado por RabbitMQ)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
