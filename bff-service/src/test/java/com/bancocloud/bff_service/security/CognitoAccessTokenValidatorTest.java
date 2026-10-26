package com.bancocloud.bff_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CognitoAccessTokenValidatorTest {

    private final CognitoAccessTokenValidator validator = new CognitoAccessTokenValidator("bancocloud-client");

    private Jwt token(String tokenUse, String clientId) {
        Jwt.Builder builder = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("sub-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (tokenUse != null) {
            builder.claim("token_use", tokenUse);
        }
        if (clientId != null) {
            builder.claim("client_id", clientId);
        }
        return builder.build();
    }

    @Test
    void aceptaAccessTokenDelClienteConfigurado() {
        assertThat(validator.validate(token("access", "bancocloud-client")).hasErrors()).isFalse();
    }

    @Test
    void rechazaIdToken() {
        assertThat(validator.validate(token("id", "bancocloud-client")).hasErrors()).isTrue();
    }

    @Test
    void rechazaOtroClientId() {
        assertThat(validator.validate(token("access", "otra-app")).hasErrors()).isTrue();
    }

    @Test
    void rechazaTokenSinClaims() {
        assertThat(validator.validate(token(null, null)).hasErrors()).isTrue();
    }
}
