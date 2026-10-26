package com.bancocloud.notificaciones_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificacionesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificacionesServiceApplication.class, args);
	}

}
