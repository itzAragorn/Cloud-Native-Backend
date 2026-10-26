package com.bancocloud.bff_service.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Validaciones propias del Access Token de Cognito, que no trae {@code aud}:
 * <ul>
 *   <li>{@code token_use} debe ser {@code access} (rechaza ID Tokens).</li>
 *   <li>{@code client_id} debe ser el App Client de Banco Cloud (rechaza tokens
 *       emitidos para otras aplicaciones del mismo User Pool).</li>
 * </ul>
 * La firma, el emisor ({@code iss}) y la expiración los valida el JwtDecoder.
 */
public class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final String clientId;

    public CognitoAccessTokenValidator(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (!"access".equals(jwt.getClaimAsString("token_use"))) {
            return error("El token no es un Access Token de Cognito");
        }
        if (!clientId.equals(jwt.getClaimAsString("client_id"))) {
            return error("El token fue emitido para otro client_id");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult error(String descripcion) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, descripcion, null));
    }
}
