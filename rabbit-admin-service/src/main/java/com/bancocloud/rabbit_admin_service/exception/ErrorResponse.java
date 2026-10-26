package com.bancocloud.rabbit_admin_service.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de error común a todas las respuestas 4xx/5xx.
 *
 * @param errores detalle por campo; solo aparece en errores de validación (400)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<CampoError> errores) {

    public record CampoError(String campo, String error) {
    }
}
