package com.bancocloud.bff_service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Reenvía la administración de Kafka (tópicos y consumer groups) a kafka-admin-service. */
@Component
public class KafkaAdminServiceClient extends ReenvioJsonClient {

    public KafkaAdminServiceClient(
            @Value("${microservices.kafka-admin.url}") String url,
            @Qualifier("microserviceRestTemplate") RestTemplate restTemplate) {
        super(url, "kafka-admin-service", "KAFKA_ADMIN_UNAVAILABLE", restTemplate);
    }
}
