package com.bancocloud.rabbit_admin_service.dto;

/** Estado actual de una cola en el broker. */
public record ColaResponse(String name, long mensajes, int consumidores) {
}
