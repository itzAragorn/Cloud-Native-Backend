package com.bancocloud.bff_service.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bancocloud.bff_service.client.KafkaAdminServiceClient;
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

/** Solo ADMIN puede usar /api/v1/admin/kafka/**; el BFF devuelve la respuesta original. */
@SpringBootTest(properties = {
        "COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test",
        "COGNITO_CLIENT_ID=test-client"
})
class KafkaAdminProxySecurityTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private KafkaAdminServiceClient client;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/v1/admin/kafka/topics")).andExpect(status().isUnauthorized());
        verifyNoInteractions(client);
    }

    @Test
    void clienteDa403() throws Exception {
        mvc.perform(get("/api/v1/admin/kafka/consumer-groups/analitica-inversiones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(client);
    }

    @Test
    void adminReenviaConNombreConPuntos() throws Exception {
        when(client.reenviar(eq(HttpMethod.GET), eq("/topics/{name}"), isNull(), eq("bancocloud.inversiones.eventos")))
                .thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"partitions\":3}"));

        mvc.perform(get("/api/v1/admin/kafka/topics/bancocloud.inversiones.eventos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"partitions\":3}"));
    }

    @Test
    void adminRecibeElErrorOriginal() throws Exception {
        String body = "{\"configs\":{\"foo\":\"1\"}}";
        when(client.reenviar(eq(HttpMethod.PUT), eq("/topics/{name}/configs"), eq(body), eq("demo")))
                .thenReturn(ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("{\"status\":400}"));

        mvc.perform(put("/api/v1/admin/kafka/topics/demo/configs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"status\":400}"));
    }
}
