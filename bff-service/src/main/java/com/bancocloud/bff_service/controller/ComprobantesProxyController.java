package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.NotificacionesServiceClient;
import com.bancocloud.bff_service.security.UsuarioPropietarioResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Comprobantes de inversión generados por notificaciones-service.
 * CLIENTE solo ve los suyos; ADMIN puede consultar cualquier usuario.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/comprobantes")
@RequiredArgsConstructor
public class ComprobantesProxyController {

    private final NotificacionesServiceClient notificacionesServiceClient;
    private final UsuarioPropietarioResolver usuarioPropietarioResolver;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENTE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerComprobantes(
            @RequestParam Long usuarioId,
            JwtAuthenticationToken authentication) {

        Long usuarioIdFinal = usuarioPropietarioResolver.obtenerUsuarioIdPermitido(usuarioId, authentication);
        if (usuarioIdFinal == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("BFF: GET /api/v1/comprobantes - Usuario {}", usuarioIdFinal);
        return ResponseEntity.ok(notificacionesServiceClient.obtenerComprobantes(usuarioIdFinal));
    }
}
