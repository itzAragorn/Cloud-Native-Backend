package com.bancocloud.inversiones_service.client;

import com.bancocloud.inversiones_service.dto.FondoDTO;
import com.bancocloud.inversiones_service.exception.FondoServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class FondoServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${fondos-service.url:http://localhost:8081}")
    private String fondosServiceUrl;
    
    /**
     * Obtener los detalles de un fondo desde el fondos-service
     */
    public FondoDTO obtenerFondo(Long fondoId) {
        try {
            log.info("Consultando fondo {} en fondos-service", fondoId);
            String url = fondosServiceUrl + "/api/v1/fondos/" + fondoId;
            FondoDTO fondo = restTemplate.getForObject(url, FondoDTO.class);
            
            if (fondo == null) {
                throw new FondoServiceException("Fondo no encontrado con id: " + fondoId);
            }
            
            log.info("Fondo obtenido correctamente: {}", fondo.getNombre());
            return fondo;
        } catch (RestClientException e) {
            log.error("Error al consultar fondo {} en fondos-service", fondoId, e);
            throw new FondoServiceException("Error al consultar fondos-service: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtener el valor actual de la cuota de un fondo
     */
    public BigDecimal obtenerValorCuota(Long fondoId) {
        try {
            log.info("Obteniendo valor de cuota para fondo {} en fondos-service", fondoId);
            String url = fondosServiceUrl + "/api/v1/fondos/" + fondoId + "/valor-cuota";
            BigDecimal valorCuota = restTemplate.getForObject(url, BigDecimal.class);
            
            if (valorCuota == null) {
                throw new FondoServiceException("No se pudo obtener el valor de cuota para fondo: " + fondoId);
            }
            
            log.info("Valor de cuota obtenido: {}", valorCuota);
            return valorCuota;
        } catch (RestClientException e) {
            log.error("Error al obtener valor de cuota para fondo {} en fondos-service", fondoId, e);
            throw new FondoServiceException("Error al consultar fondos-service: " + e.getMessage(), e);
        }
    }
}
