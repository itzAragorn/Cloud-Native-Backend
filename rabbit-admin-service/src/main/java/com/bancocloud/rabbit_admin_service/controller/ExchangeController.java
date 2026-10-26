package com.bancocloud.rabbit_admin_service.controller;

import com.bancocloud.rabbit_admin_service.dto.CrearExchangeRequest;
import com.bancocloud.rabbit_admin_service.dto.ExchangeResponse;
import com.bancocloud.rabbit_admin_service.service.RabbitAdminService;
import com.bancocloud.rabbit_admin_service.validation.NombreRecurso;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exchanges: 201 al crear, 409 si ya existe, 404 si no existe. */
@RestController
@RequestMapping("/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeResponse crear(@Valid @RequestBody CrearExchangeRequest request) {
        return rabbitAdminService.crearExchange(request);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @NombreRecurso String name) {
        rabbitAdminService.eliminarExchange(name);
    }
}
