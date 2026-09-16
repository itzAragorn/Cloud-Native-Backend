package com.bancocloud.bff_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Proveedor para generar y validar tokens JWT
 * - Genera JWT para login/autenticación
 * - Valida y extrae claims del JWT
 * - Utiliza clave secreta en lugar de Azure AD
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey jwtSecret;
    private final long jwtExpirationMs;
    private final String jwtIssuer;
    private final String jwtAudience;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience) {
        // Crear clave secreta de al menos 256 bits
        String safeSecret = secret.length() < 32 ? 
            secret + "0000000000000000000000000000000000000000" : 
            secret;
        this.jwtSecret = Keys.hmacShaKeyFor(safeSecret.getBytes());
        this.jwtExpirationMs = expirationMs;
        this.jwtIssuer = issuer;
        this.jwtAudience = audience;
    }

    /**
     * Genera un JWT para un usuario autenticado
     * @param usuarioId ID del usuario
     * @param email Email del usuario
     * @param rol Rol del usuario (ADMIN, CLIENTE)
     * @return Token JWT
     */
    public String generateToken(Long usuarioId, String email, String rol) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .claim("email", email)
                .claim("rol", rol)
                .issuer(jwtIssuer)
                .audience().add(jwtAudience).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecret)
                .compact();
    }

    /**
     * Valida un token JWT
     * @param token Token a validar
     * @return true si el token es válido, false si expiró o es inválido
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSecret)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("Token JWT expirado: {}", ex.getMessage());
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException ex) {
            log.warn("Token JWT no soportado: {}", ex.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            log.warn("Token JWT inválido: {}", ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            log.warn("String JWT vacío: {}", ex.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            log.warn("Fallo en validación de firma JWT: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extrae claims del token JWT
     * @param token Token JWT
     * @return Claims del token
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el usuarioId del token JWT
     * @param token Token JWT
     * @return usuarioId
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object usuarioId = claims.get("usuarioId");
        if (usuarioId instanceof Integer) {
            return ((Integer) usuarioId).longValue();
        }
        return (Long) usuarioId;
    }

    /**
     * Extrae el email del token JWT
     * @param token Token JWT
     * @return Email del usuario
     */
    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Extrae el rol del token JWT
     * @param token Token JWT
     * @return Rol del usuario
     */
    public String getRoleFromToken(String token) {
        return (String) getClaimsFromToken(token).get("rol");
    }
}
