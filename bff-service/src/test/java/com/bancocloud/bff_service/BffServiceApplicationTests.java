package com.bancocloud.bff_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test",
		"COGNITO_CLIENT_ID=test-client"
})
class BffServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
