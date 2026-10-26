package com.bancocloud.bff_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bancocloud.bff_service.client.RabbitAdminServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Solo ADMIN puede usar /api/v1/admin/rabbit/**; el BFF devuelve la respuesta original. */
@SpringBootTest(properties = {
        "COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test",
        "COGNITO_CLIENT_ID=test-client"
})
class RabbitAdminProxySecurityTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private RabbitAdminServiceClient client;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/v1/admin/rabbit/queues/demo.q")).andExpect(status().isUnauthorized());
        verifyNoInteractions(client);
    }

    @Test
    void clienteDa403() throws Exception {
        mvc.perform(get("/api/v1/admin/rabbit/queues/demo.q")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(client);
    }

    @Test
    void adminReenviaYDevuelveRespuestaOriginal() throws Exception {
        String body = "{\"name\":\"demo.q\"}";
        when(client.reenviar(eq(HttpMethod.POST), eq("/queues"), eq(body)))
                .thenReturn(ResponseEntity.status(409).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":409}"));

        mvc.perform(post("/api/v1/admin/rabbit/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"status\":409}"));
    }

    @Test
    void adminEliminaCola() throws Exception {
        when(client.reenviar(any(), any(), isNull(), any())).thenReturn(ResponseEntity.noContent().build());

        mvc.perform(delete("/api/v1/admin/rabbit/queues/demo.q")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
        verify(client).reenviar(HttpMethod.DELETE, "/queues/{name}", null, "demo.q");
    }
}
