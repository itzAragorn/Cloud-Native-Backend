package com.bancocloud.fondos_service.exception;

public class FondoDuplicadoException extends RuntimeException {
    
    public FondoDuplicadoException(String nombre) {
        super("Ya existe un fondo con el nombre: " + nombre);
    }
}
