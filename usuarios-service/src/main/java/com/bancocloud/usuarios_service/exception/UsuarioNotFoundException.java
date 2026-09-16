package com.bancocloud.usuarios_service.exception;

public class UsuarioNotFoundException extends RuntimeException {
    
    public UsuarioNotFoundException(Long id) {
        super("Usuario no encontrado con id: " + id);
    }
    
    public UsuarioNotFoundException(String mensaje) {
        super(mensaje);
    }
}
