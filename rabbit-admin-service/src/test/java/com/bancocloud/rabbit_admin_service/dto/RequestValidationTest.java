package com.bancocloud.rabbit_admin_service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> camposConError(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void colaValidaNoTieneErrores() {
        assertThat(camposConError(new CrearColaRequest("demo.q", true, "demo.dlx", "demo.q.dlq", 1000L))).isEmpty();
        assertThat(camposConError(new CrearColaRequest("demo_q-1", null, null, null, null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "con espacio", "tilde-á", "amq.reservada", "AMQ.Mayus", "barra/no"})
    void nombresDeColaInvalidos(String nombre) {
        assertThat(camposConError(new CrearColaRequest(nombre, true, null, null, null))).containsExactly("name");
    }

    @Test
    void nombreDeMasDe100CaracteresEsInvalido() {
        assertThat(camposConError(new CrearColaRequest("q".repeat(101), true, null, null, null)))
                .containsExactly("name");
    }

    @Test
    void nombreNuloEsInvalido() {
        assertThat(camposConError(new CrearColaRequest(null, true, null, null, null))).containsExactly("name");
    }

    @Test
    void ttlDebeSerPositivoYDentroDelLimite() {
        assertThat(camposConError(new CrearColaRequest("q", true, null, null, 0L))).containsExactly("ttlMs");
        assertThat(camposConError(new CrearColaRequest("q", true, null, null, -1L))).containsExactly("ttlMs");
        assertThat(camposConError(new CrearColaRequest("q", true, null, null, 3_000_000_000L))).containsExactly("ttlMs");
    }

    @Test
    void routingKeyDeDeadLetterRequiereExchange() {
        assertThat(camposConError(new CrearColaRequest("q", true, null, "k", null)))
                .containsExactly("deadLetterRoutingKey");
    }

    @Test
    void deadLetterExchangeReservadoEsInvalido() {
        assertThat(camposConError(new CrearColaRequest("q", true, "amq.direct", null, null)))
                .containsExactly("deadLetterExchange");
    }

    @Test
    void exchangeRequiereTipo() {
        assertThat(camposConError(new CrearExchangeRequest("x", null, true))).containsExactly("type");
        assertThat(camposConError(new CrearExchangeRequest("x", TipoExchange.HEADERS, null))).isEmpty();
    }

    @Test
    void bindingAdmiteComodinesYRoutingKeyVacia() {
        assertThat(camposConError(new BindingRequest("q", "x", "usuario.estado.*"))).isEmpty();
        assertThat(camposConError(new BindingRequest("q", "x", "#"))).isEmpty();
        assertThat(camposConError(new BindingRequest("q", "x", ""))).isEmpty();
    }

    @Test
    void bindingValidaTodosLosCampos() {
        assertThat(camposConError(new BindingRequest("", "amq.topic", null)))
                .containsExactlyInAnyOrder("queue", "exchange", "routingKey");
        assertThat(camposConError(new BindingRequest("q", "x", "con espacio"))).containsExactly("routingKey");
    }
}
