package com.bancocloud.fondos_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FondosServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FondosServiceApplication.class, args);
	}

}
