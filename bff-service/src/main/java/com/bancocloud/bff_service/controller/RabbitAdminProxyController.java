package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.RabbitAdminServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administración de RabbitMQ expuesta al frontend. Solo ADMIN.
 * Las validaciones y los códigos de respuesta los define rabbit-admin-service.
 */
@RestController
@RequestMapping("/api/v1/admin/rabbit")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class RabbitAdminProxyController {

    private final RabbitAdminServiceClient client;

    @PostMapping("/queues")
    public ResponseEntity<String> crearCola(@RequestBody String body) {
        return client.reenviar(HttpMethod.POST, "/queues", body);
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<String> obtenerCola(@PathVariable String name) {
        return client.reenviar(HttpMethod.GET, "/queues/{name}", null, name);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<String> eliminarCola(@PathVariable String name) {
        return client.reenviar(HttpMethod.DELETE, "/queues/{name}", null, name);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<String> crearExchange(@RequestBody String body) {
        return client.reenviar(HttpMethod.POST, "/exchanges", body);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<String> eliminarExchange(@PathVariable String name) {
        return client.reenviar(HttpMethod.DELETE, "/exchanges/{name}", null, name);
    }

    @PostMapping("/bindings")
    public ResponseEntity<String> crearBinding(@RequestBody String body) {
        return client.reenviar(HttpMethod.POST, "/bindings", body);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<String> eliminarBinding(@RequestBody String body) {
        return client.reenviar(HttpMethod.DELETE, "/bindings", body);
    }
}
