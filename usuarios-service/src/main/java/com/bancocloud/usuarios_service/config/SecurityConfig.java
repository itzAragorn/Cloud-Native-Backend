package com.bancocloud.usuarios_service.config;

import com.bancocloud.usuarios_service.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para usuarios-service
 * Configura autenticación por formulario usando BD
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    
    /**
     * Codificador de passwords con BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Configuración de la cadena de filtros de seguridad
     * API Backend - Permitir acceso directo a endpoints para BFF
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        http
            // Deshabilitar CSRF para APIs REST
            .csrf(csrf -> csrf.disable())
            
            // Configurar autorización
            .authorizeHttpRequests(authz -> authz
                // Permitir acceso público a health y actuator
                .requestMatchers("/health", "/actuator/**").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                
                // Permitir acceso a todos los endpoints de usuario-service (usados por BFF)
                .anyRequest().permitAll()
            );
        
        // Configurar autenticación para usar CustomUserDetailsService
        http.authenticationManager(authenticationManagerBean(http, passwordEncoder));
        
        return http.build();
    }
    
    /**
     * Configura el AuthenticationManager para usar CustomUserDetailsService
     */
    private org.springframework.security.authentication.AuthenticationManager authenticationManagerBean(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        var provider = new org.springframework.security.authentication.dao.DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }
}
