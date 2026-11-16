package com.bancocloud.kafka_admin_service.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.common.config.TopicConfig;

/**
 * Lista blanca de configuraciones de tópico que se pueden fijar por la API, con la validación de
 * cada valor. Lo que no está aquí (por ejemplo {@code unclean.leader.election.enable}) se rechaza.
 */
public final class ConfigsTopico {

    public static final Set<String> PERMITIDAS = Set.of(
            TopicConfig.RETENTION_MS_CONFIG,
            TopicConfig.CLEANUP_POLICY_CONFIG,
            TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
            TopicConfig.SEGMENT_BYTES_CONFIG,
            TopicConfig.MAX_MESSAGE_BYTES_CONFIG);

    private static final Set<String> POLITICAS = Set.of("delete", "compact", "compact,delete", "delete,compact");

    /** Mínimo de segment.bytes: segmentos más chicos generan miles de archivos. */
    private static final long SEGMENTO_MINIMO = 1_048_576L;

    private ConfigsTopico() {
    }

    /**
     * @param permitirNulos {@code true} al modificar: un valor nulo vuelve la clave a su valor por defecto
     * @return un mensaje por clave inválida, con la forma {@code "clave: motivo"}
     */
    public static List<String> errores(Map<String, String> configs, boolean permitirNulos) {
        List<String> errores = new ArrayList<>();
        if (configs == null) {
            return errores;
        }
        configs.forEach((clave, valor) -> {
            if (!PERMITIDAS.contains(clave)) {
                errores.add(clave + ": configuración no permitida; usa una de " + PERMITIDAS.stream().sorted().toList());
            } else if (valor == null) {
                if (!permitirNulos) {
                    errores.add(clave + ": no puede ser nulo");
                }
            } else {
                String error = validarValor(clave, valor.trim());
                if (error != null) {
                    errores.add(clave + ": " + error);
                }
            }
        });
        errores.sort(null);
        return errores;
    }

    private static String validarValor(String clave, String valor) {
        return switch (clave) {
            case TopicConfig.CLEANUP_POLICY_CONFIG -> POLITICAS.contains(valor.replace(" ", ""))
                    ? null : "debe ser delete, compact o compact,delete";
            // -1 = retención ilimitada
            case TopicConfig.RETENTION_MS_CONFIG -> entero(valor, -1, Long.MAX_VALUE, "debe ser -1 (sin límite) o un número de ms mayor que 0", true);
            case TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG -> entero(valor, 1, Short.MAX_VALUE, "debe ser un entero mayor o igual a 1", false);
            case TopicConfig.SEGMENT_BYTES_CONFIG -> entero(valor, SEGMENTO_MINIMO, Integer.MAX_VALUE, "debe estar entre 1048576 (1 MB) y 2147483647", false);
            case TopicConfig.MAX_MESSAGE_BYTES_CONFIG -> entero(valor, 1, Integer.MAX_VALUE, "debe ser un entero mayor que 0", false);
            default -> "configuración no permitida";
        };
    }

    private static String entero(String valor, long min, long max, String error, boolean sinCero) {
        try {
            long n = Long.parseLong(valor);
            return n < min || n > max || (sinCero && n == 0) ? error : null;
        } catch (NumberFormatException e) {
            return error;
        }
    }
}
