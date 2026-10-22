package com.bancocloud.bff_service.controller;

import com.bancocloud.bff_service.client.InversionesServiceClient;
import com.bancocloud.bff_service.client.UsuariosServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/inversiones")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:3001"
})
public class InversionesProxyController {

    private final InversionesServiceClient inversionesServiceClient;
    private final UsuariosServiceClient usuariosServiceClient;

    public InversionesProxyController(
            InversionesServiceClient inversionesServiceClient,
            UsuariosServiceClient usuariosServiceClient) {

        this.inversionesServiceClient =
                inversionesServiceClient;

        this.usuariosServiceClient =
                usuariosServiceClient;
    }

    /**
     * Crea una nueva inversión.
     *
     * CLIENTE:
     * El usuarioId siempre se obtiene desde Cognito
     * y no desde el frontend.
     *
     * ADMIN:
     * Puede utilizar el usuarioId enviado en la solicitud.
     */
    @PostMapping
    @PreAuthorize(
            "hasAuthority('ROLE_CLIENTE') or " +
            "hasAuthority('ROLE_ADMIN')"
    )
    public ResponseEntity<Map<String, Object>> crearInversion(
            @RequestBody Map<String, Object> inversionRequest,
            JwtAuthenticationToken authentication) {

        log.info(
                "BFF: POST /api/v1/inversiones - Crear inversión"
        );

        Map<String, Object> request =
                new HashMap<>(inversionRequest);

        if (authentication.getAuthorities()
                .stream()
                .anyMatch(a ->
                        a.getAuthority()
                                .equals("ROLE_CLIENTE"))) {

            String cognitoSub =
                    authentication.getToken().getSubject();

            Map<String, Object> usuario =
                    usuariosServiceClient
                            .obtenerUsuarioPorCognitoSub(
                                    cognitoSub
                            );

            if (usuario == null ||
                    usuario.get("id") == null) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error",
                                "USER_NOT_REGISTERED",
                                "message",
                                "El usuario no está registrado en BancoCloud"
                        ));
            }

            request.put(
                    "usuarioId",
                    usuario.get("id")
            );
        }

        Map<String, Object> response =
                inversionesServiceClient
                        .crearInversion(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Obtener inversiones de un usuario.
     *
     * CLIENTE:
     * Solo puede consultar sus propias inversiones.
     *
     * ADMIN:
     * Puede consultar las inversiones de cualquier usuario.
     */
    @GetMapping("/mis-inversiones")
    @PreAuthorize(
            "hasAuthority('ROLE_CLIENTE') or " +
            "hasAuthority('ROLE_ADMIN')"
    )
    public ResponseEntity<List<Map<String, Object>>>
    obtenerMisInversiones(
            @RequestParam Long usuarioId,
            JwtAuthenticationToken authentication) {

        Long usuarioIdFinal =
                obtenerUsuarioIdPermitido(
                        usuarioId,
                        authentication
                );

        if (usuarioIdFinal == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        log.info(
                "BFF: GET /api/v1/inversiones/mis-inversiones - Usuario {}",
                usuarioIdFinal
        );

        List<Map<String, Object>> response =
                inversionesServiceClient
                        .obtenerMisInversiones(
                                usuarioIdFinal
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener portafolio de un usuario.
     *
     * CLIENTE:
     * Solo puede consultar su propio portafolio.
     *
     * ADMIN:
     * Puede consultar el portafolio de cualquier usuario.
     */
    @GetMapping("/mi-portafolio")
    @PreAuthorize(
            "hasAuthority('ROLE_CLIENTE') or " +
            "hasAuthority('ROLE_ADMIN')"
    )
    public ResponseEntity<Map<String, Object>>
    obtenerMiPortafolio(
            @RequestParam Long usuarioId,
            JwtAuthenticationToken authentication) {

        Long usuarioIdFinal =
                obtenerUsuarioIdPermitido(
                        usuarioId,
                        authentication
                );

        if (usuarioIdFinal == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        log.info(
                "BFF: GET /api/v1/inversiones/mi-portafolio - Usuario {}",
                usuarioIdFinal
        );

        Map<String, Object> response =
                inversionesServiceClient
                        .obtenerMiPortafolio(
                                usuarioIdFinal
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener una inversión específica.
     *
     * CLIENTE y ADMIN pueden acceder,
     * pero la validación final de propiedad debe
     * realizarse también en Inversiones-Service.
     */
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('ROLE_CLIENTE') or " +
            "hasAuthority('ROLE_ADMIN')"
    )
    public ResponseEntity<Map<String, Object>> obtenerInversion(
            @PathVariable Long id) {

        log.info(
                "BFF: GET /api/v1/inversiones/{} - Obtener inversión",
                id
        );

        Map<String, Object> response =
                inversionesServiceClient
                        .obtenerInversion(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener rendimiento de una inversión específica.
     *
     * La validación de propiedad debe realizarse también
     * en Inversiones-Service.
     */
    @GetMapping("/{id}/rendimiento")
    @PreAuthorize(
            "hasAuthority('ROLE_CLIENTE') or " +
            "hasAuthority('ROLE_ADMIN')"
    )
    public ResponseEntity<Map<String, Object>>
    obtenerRendimientoInversion(
            @PathVariable Long id) {

        log.info(
                "BFF: GET /api/v1/inversiones/{}/rendimiento",
                id
        );

        Map<String, Object> response =
                inversionesServiceClient
                        .obtenerRendimientoInversion(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener rendimiento total de un usuario.
     *
     * CLIENTE:
     * Solo puede consultar su propio rendimiento.
     *
     * ADMIN:
     * Puede consultar el rendimiento de cualquier usuario.
     */
    @GetMapping("/rendimiento")
    @PreAuthorize(
            "hasAuthority('ROLE_CLIENTE') or " +
            "hasAuthority('ROLE_ADMIN')"
    )
    public ResponseEntity<List<Map<String, Object>>>
    obtenerRendimientoUsuario(
            @RequestParam Long usuarioId,
            JwtAuthenticationToken authentication) {

        Long usuarioIdFinal =
                obtenerUsuarioIdPermitido(
                        usuarioId,
                        authentication
                );

        if (usuarioIdFinal == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        log.info(
                "BFF: GET /api/v1/inversiones/rendimiento - Usuario {}",
                usuarioIdFinal
        );

        List<Map<String, Object>> response =
                inversionesServiceClient
                        .obtenerRendimientoUsuario(
                                usuarioIdFinal
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Determina qué usuario puede consultar el CLIENTE.
     *
     * ADMIN puede consultar cualquier usuario.
     * CLIENTE solamente puede consultar su propio ID.
     */
    private Long obtenerUsuarioIdPermitido(
            Long usuarioIdSolicitado,
            JwtAuthenticationToken authentication) {

        boolean esAdmin =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(a ->
                                a.getAuthority()
                                        .equals("ROLE_ADMIN"));

        if (esAdmin) {
            return usuarioIdSolicitado;
        }

        String cognitoSub =
                authentication.getToken().getSubject();

        Map<String, Object> usuario =
                usuariosServiceClient
                        .obtenerUsuarioPorCognitoSub(
                                cognitoSub
                        );

        if (usuario == null ||
                usuario.get("id") == null) {
            return null;
        }

        Long usuarioIdAutenticado =
                ((Number) usuario.get("id")).longValue();

        if (!usuarioIdAutenticado.equals(
                usuarioIdSolicitado)) {

            log.warn(
                    "Intento de acceso a inversiones de otro usuario. " +
                    "Usuario autenticado={}, usuario solicitado={}",
                    usuarioIdAutenticado,
                    usuarioIdSolicitado
            );

            return null;
        }

        return usuarioIdAutenticado;
    }
}