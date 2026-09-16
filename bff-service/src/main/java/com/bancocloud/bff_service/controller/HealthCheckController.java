package com.bancocloud.bff_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class HealthCheckController {

    /**
     * Endpoint de health check
     * Disponible públicamente sin autenticación
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("BFF: GET /api/v1/health - Health check");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "BFF-Service");
        response.put("version", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Información general del BFF
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        log.info("BFF: GET /api/v1/info - Service info");
        
        Map<String, Object> response = new HashMap<>();
        response.put("name", "BFF-Service (Backend for Frontend)");
        response.put("description", "API Gateway y BFF para Banco Cloud");
        response.put("version", "1.0.0");
        response.put("port", 8080);
        response.put("microservices", Map.of(
            "fondos", "http://localhost:8081",
            "inversiones", "http://localhost:8082",
            "usuarios", "http://localhost:8083"
        ));
        response.put("security", "OAuth2 + JWT");
        
        return ResponseEntity.ok(response);
    }
}
