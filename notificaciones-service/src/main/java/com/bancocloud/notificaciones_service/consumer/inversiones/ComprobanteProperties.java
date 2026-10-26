package com.bancocloud.notificaciones_service.consumer.inversiones;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param directorio carpeta (volumen en Docker) donde se guardan los comprobantes y el correlativo
 */
@Validated
@ConfigurationProperties(prefix = "app.comprobantes")
public record ComprobanteProperties(@NotBlank String directorio) {
}
