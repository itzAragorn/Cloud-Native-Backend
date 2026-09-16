package com.bancocloud.inversiones_service.exception;

public class FondoServiceException extends RuntimeException {
    
    public FondoServiceException(String mensaje) {
        super(mensaje);
    }
    
    public FondoServiceException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
