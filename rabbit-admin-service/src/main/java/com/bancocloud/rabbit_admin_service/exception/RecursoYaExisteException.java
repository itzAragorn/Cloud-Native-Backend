package com.bancocloud.rabbit_admin_service.exception;

/** Ya existe una cola o exchange con ese nombre (HTTP 409). */
public class RecursoYaExisteException extends RuntimeException {

    public RecursoYaExisteException(String tipo, String nombre) {
        super("Ya existe " + tipo + " '" + nombre + "'");
    }
}
