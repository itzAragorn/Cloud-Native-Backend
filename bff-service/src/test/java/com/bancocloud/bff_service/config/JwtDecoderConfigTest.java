package com.bancocloud.bff_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

/** Decodificador con JWKS fijo (COGNITO_JWKS): valida igual que con Cognito, sin red. */
class JwtDecoderConfigTest {

    private static final String ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_PRUEBA";
    private static final String CLIENT_ID = "bancocloud-client";

    private static RSAKey clave;
    private static RSAKey otraClave;
    private static JwtDecoder decoder;

    @BeforeAll
    static void configurar() throws Exception {
        clave = new RSAKeyGenerator(2048).keyID("clave-1").generate();
        otraClave = new RSAKeyGenerator(2048).keyID("clave-1").generate();
        String jwks = new JWKSet(clave.toPublicJWK()).toString();
        decoder = new JwtDecoderConfig().jwtDecoder(ISSUER, CLIENT_ID, jwks);
    }

    private static String token(RSAKey firma, String issuer, String clientId, Instant expira) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sub-123")
                .issuer(issuer)
                .claim("token_use", "access")
                .claim("client_id", clientId)
                .issueTime(Date.from(expira.minusSeconds(3600)))
                .expirationTime(Date.from(expira))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(firma.getKeyID()).type(JOSEObjectType.JWT).build(),
                claims);
        jwt.sign(new RSASSASigner(firma));
        return jwt.serialize();
    }

    @Test
    void aceptaTokenValidoSinConsultarCognito() throws Exception {
        Jwt jwt = decoder.decode(token(clave, ISSUER, CLIENT_ID, Instant.now().plusSeconds(300)));

        assertThat(jwt.getSubject()).isEqualTo("sub-123");
    }

    @Test
    void rechazaFirmaDeOtraClave() throws Exception {
        String falsificado = token(otraClave, ISSUER, CLIENT_ID, Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(falsificado)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void rechazaOtroIssuer() throws Exception {
        String ajeno = token(clave, "https://otro-issuer", CLIENT_ID, Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(ajeno)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rechazaOtroClientId() throws Exception {
        String ajeno = token(clave, ISSUER, "otra-app", Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(ajeno)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rechazaTokenExpirado() throws Exception {
        String vencido = token(clave, ISSUER, CLIENT_ID, Instant.now().minusSeconds(600));

        assertThatThrownBy(() -> decoder.decode(vencido)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rechazaTokenMalformadoAlInstante() {
        assertThatThrownBy(() -> decoder.decode("abc.def.ghi")).isInstanceOf(BadJwtException.class);
    }

    @Test
    void sinJwksUsaLaDescargaDesdeCognito() {
        assertThat(new JwtDecoderConfig().jwtDecoder(ISSUER, CLIENT_ID, ""))
                .isInstanceOf(SupplierJwtDecoder.class);
    }

    @Test
    void jwksInvalidoFallaAlArrancar() {
        assertThatThrownBy(() -> new JwtDecoderConfig().jwtDecoder(ISSUER, CLIENT_ID, "{no es json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COGNITO_JWKS");
    }
}
