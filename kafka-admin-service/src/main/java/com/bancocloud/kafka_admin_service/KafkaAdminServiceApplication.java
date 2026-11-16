package com.bancocloud.kafka_admin_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KafkaAdminServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaAdminServiceApplication.class, args);
	}

}
