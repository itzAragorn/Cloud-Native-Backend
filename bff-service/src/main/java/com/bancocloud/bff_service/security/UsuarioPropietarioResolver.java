package com.bancocloud.bff_service.security;

import com.bancocloud.bff_service.client.UsuariosServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Decide qué usuario puede consultar quien hace la petición.
 * <p>
 * ADMIN puede consultar cualquier usuario.
 * CLIENTE solamente puede consultar su propio ID, que se obtiene a partir del
 * {@code sub} de su token de Cognito.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioPropietarioResolver {

    private final UsuariosServiceClient usuariosServiceClient;

    /**
     * @return el usuarioId permitido, o {@code null} si la petición no está autorizada
     */
    public Long obtenerUsuarioIdPermitido(
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
                    "Intento de acceso a datos de otro usuario. " +
                    "Usuario autenticado={}, usuario solicitado={}",
                    usuarioIdAutenticado,
                    usuarioIdSolicitado
            );

            return null;
        }

        return usuarioIdAutenticado;
    }
}
