package com.bancocloud.notificaciones_service.consumer.inversiones;

import static com.bancocloud.notificaciones_service.TestFixtures.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bancocloud.notificaciones_service.exception.MensajeRecuperableException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ComprobanteServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T18:00:00Z"), ZoneId.of("UTC"));

    @TempDir
    Path dir;

    private ComprobanteService servicio(Path directorio) {
        return new ComprobanteService(JSON, new ComprobanteProperties(directorio.toString()), CLOCK);
    }

    private InversionCreadaEvent evento(long inversionId) {
        return new InversionCreadaEvent("evt-" + inversionId, inversionId, 1L, 2L,
                new BigDecimal("100000.00"), new BigDecimal("80.0000"), new BigDecimal("1250.00"),
                LocalDateTime.of(2026, 10, 26, 15, 0));
    }

    @Test
    void generaNumerosCorrelativosYArchivoJson() throws Exception {
        ComprobanteService service = servicio(dir);

        Comprobante primero = service.generar(evento(10));
        Comprobante segundo = service.generar(evento(11));

        assertThat(primero.numero()).isEqualTo("BC-2026-000001");
        assertThat(segundo.numero()).isEqualTo("BC-2026-000002");
        assertThat(Files.readString(dir.resolve("BC-2026-000001.json")))
                .contains("\"inversionId\"").contains("10");
    }

    @Test
    void elCorrelativoSobreviveAReinicios() {
        servicio(dir).generar(evento(10));

        Comprobante tras = servicio(dir).generar(evento(11));

        assertThat(tras.numero()).isEqualTo("BC-2026-000002");
    }

    @Test
    void errorDeDiscoEsRecuperable() throws Exception {
        Path archivo = Files.writeString(dir.resolve("no-es-directorio"), "x");

        assertThatThrownBy(() -> servicio(archivo).generar(evento(10)))
                .isInstanceOf(MensajeRecuperableException.class);
    }
}
