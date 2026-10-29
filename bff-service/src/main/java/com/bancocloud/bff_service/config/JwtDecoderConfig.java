package com.bancocloud.bff_service.config;

import com.bancocloud.bff_service.security.CognitoAccessTokenValidator;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

import java.text.ParseException;

/**
 * Decodificador de JWT de Cognito: firma (JWKS del User Pool), {@code iss},
 * {@code exp}/{@code nbf}, más {@code token_use} y {@code client_id}.
 * <p>
 * Las claves de firma se obtienen de una de dos formas:
 * <ul>
 *   <li>Por defecto, de Cognito: la configuración del User Pool se descarga con el primer
 *       token, así el BFF arranca aunque Cognito no esté accesible en ese momento.</li>
 *   <li>Con {@code app.cognito.jwks} (variable {@code COGNITO_JWKS}), del JSON configurado.
 *       Sirve en servidores sin salida a internet, donde descargar las claves dejaría cada
 *       petición autenticada esperando hasta agotar el tiempo. Cognito no rota estas claves
 *       por su cuenta, así que el JSON se actualiza solo si se recrea el User Pool.</li>
 * </ul>
 * Las validaciones son las mismas en ambos casos.
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${app.cognito.client-id}") String clientId,
            @Value("${app.cognito.jwks:}") String jwks) {
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new CognitoAccessTokenValidator(clientId));

        if (!jwks.isBlank()) {
            NimbusJwtDecoder decoder = new NimbusJwtDecoder(procesadorConClavesFijas(jwks));
            decoder.setJwtValidator(validator);
            return decoder;
        }

        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
            decoder.setJwtValidator(validator);
            return decoder;
        });
    }

    private static DefaultJWTProcessor<SecurityContext> procesadorConClavesFijas(String jwks) {
        JWKSet claves;
        try {
            claves = JWKSet.parse(jwks);
        } catch (ParseException e) {
            throw new IllegalStateException("COGNITO_JWKS no es un JWKS válido: " + e.getMessage(), e);
        }
        if (claves.getKeys().isEmpty()) {
            throw new IllegalStateException("COGNITO_JWKS no contiene claves");
        }

        DefaultJWTProcessor<SecurityContext> procesador = new DefaultJWTProcessor<>();
        procesador.setJWSKeySelector(new JWSVerificationKeySelector<>(
                JWSAlgorithm.RS256, new ImmutableJWKSet<>(claves)));
        // Los claims (iss, exp, token_use, client_id) los valida Spring con el validador de arriba
        procesador.setJWTClaimsSetVerifier((claims, contexto) -> { });
        return procesador;
    }
}
