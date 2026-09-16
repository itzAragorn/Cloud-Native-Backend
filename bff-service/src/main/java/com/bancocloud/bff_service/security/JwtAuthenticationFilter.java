package com.bancocloud.bff_service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Filtro JWT para validar tokens en cada request
 * - Extrae el token del header Authorization
 * - Valida el token usando JwtTokenProvider
 * - Crea una Authentication con el usuario del token
 * - Permite que Spring Security procese la autorización
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Token válido - extraer información del usuario
                Long usuarioId = tokenProvider.getUserIdFromToken(jwt);
                String email = tokenProvider.getEmailFromToken(jwt);
                String rol = tokenProvider.getRoleFromToken(jwt);

                log.debug("Usuario autenticado: {} (ID: {}, Rol: {})", email, usuarioId, rol);

                // Crear authentication token
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + rol);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(authority)
                );

                // Guardar en SecurityContext para que Spring Security lo use
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Guardar usuarioId en request attribute para acceso posterior
                request.setAttribute("usuarioId", usuarioId);
                request.setAttribute("email", email);
                request.setAttribute("rol", rol);
            }
        } catch (Exception ex) {
            log.error("No se pudo establecer autenticación del usuario: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el JWT del header Authorization
     * Espera formato: "Bearer <token>"
     * @param request HttpServletRequest
     * @return Token JWT o null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
