package com.bancocloud.inversiones_service.exception;

public class InversionNotFoundException extends RuntimeException {
    
    public InversionNotFoundException(Long id) {
        super("Inversión no encontrada con id: " + id);
    }
    
    public InversionNotFoundException(String mensaje) {
        super(mensaje);
    }
}
