package com.bancocloud.bff_service.config;

import com.bancocloud.bff_service.security.CognitoAccessTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

/**
 * Decodificador de JWT de Cognito: firma (JWKS del User Pool), {@code iss},
 * {@code exp}/{@code nbf}, más {@code token_use} y {@code client_id}.
 * <p>
 * Es perezoso: la configuración del User Pool se descarga con el primer token,
 * así el BFF arranca aunque Cognito no esté accesible en ese momento.
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${app.cognito.client-id}") String clientId) {
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(issuer),
                    new CognitoAccessTokenValidator(clientId)));
            return decoder;
        });
    }
}
