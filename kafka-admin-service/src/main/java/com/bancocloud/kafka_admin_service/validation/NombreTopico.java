package com.bancocloud.kafka_admin_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nombre válido para un tópico de Kafka: obligatorio, máximo 249 caracteres (límite de Kafka),
 * solo letras, números, punto, guion y guion bajo, y sin el prefijo {@code __} de los tópicos
 * internos ({@code __consumer_offsets}).
 */
@Documented
@NotBlank(message = "es obligatorio")
@Size(max = 249, message = "debe tener como máximo 249 caracteres")
@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "solo admite letras, números, '.', '_' y '-'")
@Pattern(regexp = "^(?!__).*$", message = "no puede empezar con '__' (reservado para tópicos internos)")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NombreTopico {

    String message() default "nombre de tópico inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
