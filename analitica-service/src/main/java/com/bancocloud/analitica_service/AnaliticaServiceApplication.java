package com.bancocloud.analitica_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnaliticaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnaliticaServiceApplication.class, args);
	}

}
