package com.bancocloud.rabbit_admin_service.exception;

/** La cola o el exchange indicado no existe en el broker (HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String tipo, String nombre) {
        super("No existe " + tipo + " '" + nombre + "'");
    }
}
