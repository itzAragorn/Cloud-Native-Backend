package com.bancocloud.rabbit_admin_service.controller;

import com.bancocloud.rabbit_admin_service.dto.ColaResponse;
import com.bancocloud.rabbit_admin_service.dto.CrearColaRequest;
import com.bancocloud.rabbit_admin_service.service.RabbitAdminService;
import com.bancocloud.rabbit_admin_service.validation.NombreRecurso;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Colas: 201 al crear, 409 si ya existe, 404 si no existe. */
@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
public class QueueController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColaResponse crear(@Valid @RequestBody CrearColaRequest request) {
        return rabbitAdminService.crearCola(request);
    }

    @GetMapping("/{name}")
    public ColaResponse obtener(@PathVariable @NombreRecurso String name) {
        return rabbitAdminService.obtenerCola(name);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @NombreRecurso String name) {
        rabbitAdminService.eliminarCola(name);
    }
}
