package com.bancocloud.fondos_service.exception;

public class FondoNotFoundException extends RuntimeException {
    
    public FondoNotFoundException(Long id) {
        super("Fondo no encontrado con id: " + id);
    }
    
    public FondoNotFoundException(String mensaje) {
        super(mensaje);
    }
}
