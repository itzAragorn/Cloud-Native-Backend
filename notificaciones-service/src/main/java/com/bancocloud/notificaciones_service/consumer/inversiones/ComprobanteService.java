package com.bancocloud.notificaciones_service.consumer.inversiones;

import com.bancocloud.notificaciones_service.exception.MensajeRecuperableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Genera el comprobante de cada inversión: número correlativo {@code BC-<año>-<000001>},
 * archivo JSON en el directorio de comprobantes y registro en log.
 * <p>
 * El correlativo se guarda en {@code correlativo.txt} dentro del mismo directorio,
 * así sobrevive a reinicios cuando el directorio es un volumen.
 * Los errores de disco son recuperables: el mensaje se reintenta.
 */
@Slf4j
@Service
public class ComprobanteService {

    private static final String ARCHIVO_CORRELATIVO = "correlativo.txt";

    private final JsonMapper jsonMapper;
    private final Path directorio;
    private final Clock clock;

    @Autowired
    public ComprobanteService(JsonMapper jsonMapper, ComprobanteProperties properties) {
        this(jsonMapper, properties, Clock.systemDefaultZone());
    }

    ComprobanteService(JsonMapper jsonMapper, ComprobanteProperties properties, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.directorio = Path.of(properties.directorio());
        this.clock = clock;
    }

    public synchronized Comprobante generar(InversionCreadaEvent event) {
        try {
            Files.createDirectories(directorio);
            long correlativo = siguienteCorrelativo();
            LocalDateTime ahora = LocalDateTime.now(clock);
            String numero = String.format("BC-%d-%06d", ahora.getYear(), correlativo);

            Comprobante comprobante = new Comprobante(numero, ahora, event.eventId(),
                    event.inversionId(), event.usuarioId(), event.fondoId(),
                    event.monto(), event.cuotas(), event.valorCuota(), event.fecha());

            escribirAtomico(directorio.resolve(numero + ".json"),
                    jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(comprobante));
            escribirAtomico(directorio.resolve(ARCHIVO_CORRELATIVO), Long.toString(correlativo));

            log.info("Comprobante {} emitido: inversionId={} usuarioId={} fondoId={} monto={} cuotas={}",
                    numero, event.inversionId(), event.usuarioId(), event.fondoId(),
                    event.monto(), event.cuotas());
            return comprobante;
        } catch (IOException | UncheckedIOException e) {
            throw new MensajeRecuperableException("no se pudo guardar el comprobante en " + directorio, e);
        }
    }

    private long siguienteCorrelativo() throws IOException {
        Path archivo = directorio.resolve(ARCHIVO_CORRELATIVO);
        if (!Files.exists(archivo)) {
            return 1;
        }
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8).trim();
        try {
            return Long.parseLong(contenido) + 1;
        } catch (NumberFormatException e) {
            throw new IOException("correlativo.txt corrupto: '" + contenido + "'", e);
        }
    }

    private void escribirAtomico(Path destino, String contenido) throws IOException {
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.writeString(temporal, contenido, StandardCharsets.UTF_8);
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
