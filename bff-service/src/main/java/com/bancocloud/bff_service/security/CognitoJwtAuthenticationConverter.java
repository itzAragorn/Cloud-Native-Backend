package com.bancocloud.bff_service.security;

import com.bancocloud.bff_service.client.UsuariosServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CognitoJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuariosServiceClient usuariosServiceClient;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String cognitoSub = jwt.getSubject();

        if (cognitoSub == null || cognitoSub.isBlank()) {
            log.warn("JWT de Cognito sin sub");

            return new JwtAuthenticationToken(
                    jwt,
                    Collections.emptyList()
            );
        }

        try {
            Map<String, Object> usuario =
                    usuariosServiceClient
                            .obtenerUsuarioPorCognitoSub(cognitoSub);

            /*
             * El usuario existe en Cognito pero todavía no
             * está registrado en BancoCloud.
             *
             * El JWT sigue siendo válido, pero no tendrá
             * permisos dentro de BancoCloud.
             */
            if (usuario == null) {

                log.info(
                        "Usuario Cognito con sub {} no está registrado en BancoCloud",
                        cognitoSub
                );

                return new JwtAuthenticationToken(
                        jwt,
                        Collections.emptyList(),
                        cognitoSub
                );
            }

            String rol =
                    String.valueOf(usuario.get("rol"));

            Boolean accesoAutorizado =
                    (Boolean) usuario.get("accesoAutorizado");

            String estado =
                    String.valueOf(usuario.get("estado"));

            boolean accesoValido =
                    Boolean.TRUE.equals(accesoAutorizado)
                            && "ACTIVO".equalsIgnoreCase(estado);

            if (!accesoValido) {

                log.warn(
                        "Usuario BancoCloud no autorizado. " +
                        "Sub={}, estado={}, acceso={}",
                        cognitoSub,
                        estado,
                        accesoAutorizado
                );

                return new JwtAuthenticationToken(
                        jwt,
                        Collections.emptyList(),
                        cognitoSub
                );
            }

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + rol.toUpperCase()
                    );

            log.info(
                    "Usuario Cognito {} autenticado como {}",
                    cognitoSub,
                    authority.getAuthority()
            );

            return new JwtAuthenticationToken(
                    jwt,
                    Collections.singletonList(authority),
                    cognitoSub
            );

        } catch (Exception ex) {

            log.error(
                    "Error consultando usuario BancoCloud " +
                    "para Cognito Sub {}: {}",
                    cognitoSub,
                    ex.getMessage()
            );

            return new JwtAuthenticationToken(
                    jwt,
                    Collections.emptyList(),
                    cognitoSub
            );
        }
    }
}