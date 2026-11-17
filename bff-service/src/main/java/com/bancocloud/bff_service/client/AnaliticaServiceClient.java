package com.bancocloud.bff_service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Reenvía las consultas de analítica (métricas armadas desde Kafka) a analitica-service. */
@Component
public class AnaliticaServiceClient extends ReenvioJsonClient {

    public AnaliticaServiceClient(
            @Value("${microservices.analitica.url}") String url,
            @Qualifier("microserviceRestTemplate") RestTemplate restTemplate) {
        super(url, "analitica-service", "ANALITICA_UNAVAILABLE", restTemplate);
    }
}
