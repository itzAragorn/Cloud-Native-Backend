package com.bancocloud.bff_service.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bancocloud.bff_service.client.AnaliticaServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** El resumen de analítica es solo ADMIN; el historial del valor cuota también lo ve un CLIENTE. */
@SpringBootTest(properties = {
        "COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test",
        "COGNITO_CLIENT_ID=test-client"
})
class AnaliticaProxySecurityTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AnaliticaServiceClient client;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/v1/analitica/fondos/resumen")).andExpect(status().isUnauthorized());
        verifyNoInteractions(client);
    }

    @Test
    void clienteNoVeElResumen() throws Exception {
        mvc.perform(get("/api/v1/analitica/fondos/resumen")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(client);
    }

    @Test
    void clienteVeElHistorialDelValorCuota() throws Exception {
        when(client.reenviar(eq(HttpMethod.GET), eq("/analitica/fondos/{id}/valor-cuota/historial"), isNull(), eq(5L)))
                .thenReturn(ResponseEntity.ok("[]"));

        mvc.perform(get("/api/v1/analitica/fondos/5/valor-cuota/historial")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isOk());
    }

    @Test
    void adminVeElResumen() throws Exception {
        when(client.reenviar(eq(HttpMethod.GET), eq("/analitica/fondos/resumen"), isNull()))
                .thenReturn(ResponseEntity.ok("[]"));

        mvc.perform(get("/api/v1/analitica/fondos/resumen")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
