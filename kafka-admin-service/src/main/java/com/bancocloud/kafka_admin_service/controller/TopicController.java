package com.bancocloud.kafka_admin_service.controller;

import com.bancocloud.kafka_admin_service.dto.AumentarParticionesRequest;
import com.bancocloud.kafka_admin_service.dto.CrearTopicoRequest;
import com.bancocloud.kafka_admin_service.dto.ModificarConfigsRequest;
import com.bancocloud.kafka_admin_service.dto.TopicoDetalle;
import com.bancocloud.kafka_admin_service.dto.TopicoResumen;
import com.bancocloud.kafka_admin_service.service.KafkaAdminService;
import com.bancocloud.kafka_admin_service.validation.NombreTopico;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tópicos: 201 al crear, 404 si no existe, 409 si ya existe, 503 si Kafka no responde. */
@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicController {

    private final KafkaAdminService service;

    @GetMapping
    public List<TopicoResumen> listar() {
        return service.listarTopicos();
    }

    @GetMapping("/{name}")
    public TopicoDetalle obtener(@PathVariable @NombreTopico String name) {
        return service.obtenerTopico(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicoDetalle crear(@Valid @RequestBody CrearTopicoRequest request) {
        return service.crearTopico(request);
    }

    @PutMapping("/{name}/configs")
    public TopicoDetalle modificarConfigs(@PathVariable @NombreTopico String name,
                                          @Valid @RequestBody ModificarConfigsRequest request) {
        return service.modificarConfigs(name, request.configs());
    }

    @PostMapping("/{name}/partitions")
    public TopicoDetalle aumentarParticiones(@PathVariable @NombreTopico String name,
                                             @Valid @RequestBody AumentarParticionesRequest request) {
        return service.aumentarParticiones(name, request.total());
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @NombreTopico String name) {
        service.eliminarTopico(name);
    }
}
