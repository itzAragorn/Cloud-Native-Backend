package com.bancocloud.kafka_admin_service.controller;

import com.bancocloud.kafka_admin_service.dto.GrupoDetalle;
import com.bancocloud.kafka_admin_service.dto.GrupoResumen;
import com.bancocloud.kafka_admin_service.dto.ResetOffsetsRequest;
import com.bancocloud.kafka_admin_service.service.KafkaAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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

/** Consumer groups: estado, lag, reset de offsets (grupo detenido) y eliminación (grupo inactivo). */
@RestController
@RequestMapping("/consumer-groups")
@RequiredArgsConstructor
public class ConsumerGroupController {

    private final KafkaAdminService service;

    @GetMapping
    public List<GrupoResumen> listar() {
        return service.listarGrupos();
    }

    @GetMapping("/{id}")
    public GrupoDetalle obtener(@PathVariable @NotBlank @Size(max = 255) String id) {
        return service.obtenerGrupo(id);
    }

    @PostMapping("/{id}/reset-offsets")
    public GrupoDetalle resetearOffsets(@PathVariable @NotBlank @Size(max = 255) String id,
                                        @Valid @RequestBody ResetOffsetsRequest request) {
        return service.resetearOffsets(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @NotBlank @Size(max = 255) String id) {
        service.eliminarGrupo(id);
    }
}
