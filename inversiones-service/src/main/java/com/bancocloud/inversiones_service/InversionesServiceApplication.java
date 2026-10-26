package com.bancocloud.inversiones_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InversionesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InversionesServiceApplication.class, args);
	}

}
