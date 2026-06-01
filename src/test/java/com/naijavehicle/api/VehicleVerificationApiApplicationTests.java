package com.naijavehicle.api;

import com.naijavehicle.api.service.GoogleAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class VehicleVerificationApiApplicationTests {

	@MockitoBean
	GoogleAuthService googleAuthService;

	@Test
	void contextLoads() {
	}

}
