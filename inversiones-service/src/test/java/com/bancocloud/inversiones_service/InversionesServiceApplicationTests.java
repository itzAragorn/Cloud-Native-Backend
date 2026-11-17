package com.bancocloud.inversiones_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		// In-memory H2 instead of RDS so the build does not depend on AWS
		"INVERSIONES_DB_URL=jdbc:h2:mem:inversionestest",
		"DB_USERNAME=sa",
		"DB_PASSWORD=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		// No Kafka broker in the build: do not try to create the topics on startup
		"spring.kafka.admin.auto-create=false"
})
class InversionesServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
