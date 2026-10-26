package com.bancocloud.rabbit_admin_service.controller;

import com.bancocloud.rabbit_admin_service.dto.BindingRequest;
import com.bancocloud.rabbit_admin_service.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Bindings cola-exchange: 201 al crear, 404 si falta la cola o el exchange. */
@RestController
@RequestMapping("/bindings")
@RequiredArgsConstructor
public class BindingController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingRequest crear(@Valid @RequestBody BindingRequest request) {
        return rabbitAdminService.crearBinding(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.eliminarBinding(request);
    }
}
