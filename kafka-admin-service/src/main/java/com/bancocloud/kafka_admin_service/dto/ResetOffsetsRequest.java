package com.bancocloud.kafka_admin_service.dto;

import com.bancocloud.kafka_admin_service.validation.NombreTopico;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cuerpo de {@code POST /consumer-groups/{id}/reset-offsets}.
 *
 * @param partition opcional: solo esa partición; sin ella, todas las del tópico
 * @param offset    obligatorio con {@code OFFSET}
 */
public record ResetOffsetsRequest(
        @NombreTopico String topic,
        @NotNull(message = "es obligatorio; usa EARLIEST, LATEST u OFFSET") EstrategiaReset estrategia,
        @Min(value = 0, message = "debe ser 0 o mayor") Integer partition,
        @PositiveOrZero(message = "debe ser 0 o mayor") Long offset) {

    public enum EstrategiaReset { EARLIEST, LATEST, OFFSET }

    @AssertTrue(message = "offset es obligatorio con la estrategia OFFSET y no se usa con las demás")
    public boolean isOffsetCoherente() {
        return estrategia == null || (estrategia == EstrategiaReset.OFFSET) == (offset != null);
    }
}
