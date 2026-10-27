package com.bancocloud.bff_service.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bancocloud.bff_service.client.NotificacionesServiceClient;
import com.bancocloud.bff_service.client.UsuariosServiceClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Un CLIENTE solo puede ver sus propios comprobantes; un ADMIN puede ver los de cualquiera. */
@SpringBootTest(properties = {
        "COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test",
        "COGNITO_CLIENT_ID=test-client"
})
class ComprobantesProxySecurityTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private NotificacionesServiceClient notificacionesClient;

    @MockitoBean
    private UsuariosServiceClient usuariosClient;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        when(usuariosClient.obtenerUsuarioPorCognitoSub("sub-cliente-1")).thenReturn(Map.of("id", 1));
        when(notificacionesClient.obtenerComprobantes(anyLong()))
                .thenReturn(List.of(Map.of("numero", "BC-2026-000001")));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor cliente() {
        return jwt().jwt(j -> j.subject("sub-cliente-1"))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/v1/comprobantes").param("usuarioId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteVeSusComprobantes() throws Exception {
        mvc.perform(get("/api/v1/comprobantes").param("usuarioId", "1").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("BC-2026-000001"));
        verify(notificacionesClient).obtenerComprobantes(1L);
    }

    @Test
    void clienteNoPuedeVerComprobantesDeOtro() throws Exception {
        mvc.perform(get("/api/v1/comprobantes").param("usuarioId", "2").with(cliente()))
                .andExpect(status().isForbidden());
        verify(notificacionesClient, never()).obtenerComprobantes(anyLong());
    }

    @Test
    void adminPuedeVerComprobantesDeCualquierUsuario() throws Exception {
        mvc.perform(get("/api/v1/comprobantes").param("usuarioId", "7")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        verify(notificacionesClient).obtenerComprobantes(7L);
    }
}
