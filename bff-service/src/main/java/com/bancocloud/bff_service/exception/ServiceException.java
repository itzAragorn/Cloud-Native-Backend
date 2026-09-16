package com.bancocloud.bff_service.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    private final String serviceName;

    public ServiceException(String message, String errorCode, int httpStatus, String serviceName) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.serviceName = serviceName;
    }

    public ServiceException(String message, String errorCode, int httpStatus, String serviceName, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.serviceName = serviceName;
    }
}
