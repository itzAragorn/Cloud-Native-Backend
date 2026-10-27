package com.bancocloud.notificaciones_service.consumer.inversiones;

import com.bancocloud.notificaciones_service.exception.MensajeRecuperableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Genera el comprobante de cada inversión: número correlativo {@code BC-<año>-<000001>},
 * archivo JSON en el directorio de comprobantes y registro en log.
 * <p>
 * El correlativo se guarda en {@code correlativo.txt} dentro del mismo directorio,
 * así sobrevive a reinicios cuando el directorio es un volumen.
 * Los errores de disco son recuperables: el mensaje se reintenta.
 * <p>
 * Es idempotente por inversión: si RabbitMQ entrega dos veces el mismo evento
 * (reintento o redelivery), se devuelve el comprobante ya emitido en vez de crear otro.
 * Para eso mantiene un índice en memoria que se reconstruye desde el directorio al arrancar.
 */
@Slf4j
@Service
public class ComprobanteService {

    private static final String ARCHIVO_CORRELATIVO = "correlativo.txt";
    private static final String PATRON_COMPROBANTES = "BC-*.json";

    private final JsonMapper jsonMapper;
    private final Path directorio;
    private final Clock clock;
    private final Map<Long, Comprobante> porInversion = new ConcurrentHashMap<>();

    @Autowired
    public ComprobanteService(JsonMapper jsonMapper, ComprobanteProperties properties) {
        this(jsonMapper, properties, Clock.systemDefaultZone());
    }

    ComprobanteService(JsonMapper jsonMapper, ComprobanteProperties properties, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.directorio = Path.of(properties.directorio());
        this.clock = clock;
        cargarIndice();
    }

    public synchronized Comprobante generar(InversionCreadaEvent event) {
        Comprobante existente = porInversion.get(event.inversionId());
        if (existente != null) {
            log.warn("Evento {} duplicado: la inversión {} ya tiene el comprobante {}",
                    event.eventId(), event.inversionId(), existente.numero());
            return existente;
        }
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
            porInversion.put(comprobante.inversionId(), comprobante);

            log.info("Comprobante {} emitido: inversionId={} usuarioId={} fondoId={} monto={} cuotas={}",
                    numero, event.inversionId(), event.usuarioId(), event.fondoId(),
                    event.monto(), event.cuotas());
            return comprobante;
        } catch (IOException | UncheckedIOException e) {
            throw new MensajeRecuperableException("no se pudo guardar el comprobante en " + directorio, e);
        }
    }

    /** Comprobantes de un usuario, del más reciente al más antiguo. */
    public List<Comprobante> listarPorUsuario(Long usuarioId) {
        return porInversion.values().stream()
                .filter(c -> c.usuarioId().equals(usuarioId))
                .sorted(Comparator.comparing(Comprobante::numero).reversed())
                .toList();
    }

    public Optional<Comprobante> buscarPorInversion(Long inversionId) {
        return Optional.ofNullable(porInversion.get(inversionId));
    }

    private void cargarIndice() {
        if (!Files.isDirectory(directorio)) {
            return;
        }
        try (DirectoryStream<Path> archivos = Files.newDirectoryStream(directorio, PATRON_COMPROBANTES)) {
            for (Path archivo : archivos) {
                try {
                    Comprobante c = jsonMapper.readValue(Files.readAllBytes(archivo), Comprobante.class);
                    porInversion.put(c.inversionId(), c);
                } catch (IOException | JacksonException e) {
                    log.warn("Comprobante ilegible, se omite del índice: {} ({})", archivo, e.getMessage());
                }
            }
            log.info("Índice de comprobantes cargado: {} comprobantes en {}", porInversion.size(), directorio);
        } catch (IOException e) {
            log.error("No se pudo leer el directorio de comprobantes {}", directorio, e);
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
