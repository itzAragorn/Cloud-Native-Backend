package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.KafkaAdminServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administración de Kafka expuesta al frontend. Solo ADMIN.
 * Las validaciones y los códigos de respuesta los define kafka-admin-service.
 */
@RestController
@RequestMapping("/api/v1/admin/kafka")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class KafkaAdminProxyController {

    private final KafkaAdminServiceClient client;

    @GetMapping("/topics")
    public ResponseEntity<String> listarTopicos() {
        return client.reenviar(HttpMethod.GET, "/topics", null);
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<String> obtenerTopico(@PathVariable String name) {
        return client.reenviar(HttpMethod.GET, "/topics/{name}", null, name);
    }

    @PostMapping("/topics")
    public ResponseEntity<String> crearTopico(@RequestBody String body) {
        return client.reenviar(HttpMethod.POST, "/topics", body);
    }

    @PutMapping("/topics/{name}/configs")
    public ResponseEntity<String> modificarConfigs(@PathVariable String name, @RequestBody String body) {
        return client.reenviar(HttpMethod.PUT, "/topics/{name}/configs", body, name);
    }

    @PostMapping("/topics/{name}/partitions")
    public ResponseEntity<String> aumentarParticiones(@PathVariable String name, @RequestBody String body) {
        return client.reenviar(HttpMethod.POST, "/topics/{name}/partitions", body, name);
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<String> eliminarTopico(@PathVariable String name) {
        return client.reenviar(HttpMethod.DELETE, "/topics/{name}", null, name);
    }

    @GetMapping("/consumer-groups")
    public ResponseEntity<String> listarGrupos() {
        return client.reenviar(HttpMethod.GET, "/consumer-groups", null);
    }

    @GetMapping("/consumer-groups/{id}")
    public ResponseEntity<String> obtenerGrupo(@PathVariable String id) {
        return client.reenviar(HttpMethod.GET, "/consumer-groups/{id}", null, id);
    }

    @PostMapping("/consumer-groups/{id}/reset-offsets")
    public ResponseEntity<String> resetearOffsets(@PathVariable String id, @RequestBody String body) {
        return client.reenviar(HttpMethod.POST, "/consumer-groups/{id}/reset-offsets", body, id);
    }

    @DeleteMapping("/consumer-groups/{id}")
    public ResponseEntity<String> eliminarGrupo(@PathVariable String id) {
        return client.reenviar(HttpMethod.DELETE, "/consumer-groups/{id}", null, id);
    }
}
