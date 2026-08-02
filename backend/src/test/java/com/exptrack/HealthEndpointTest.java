package com.exptrack;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.datasource.url=jdbc:sqlite::memory:")
class HealthEndpointTest {

	@LocalServerPort
	private int port;

	@Autowired
	private Flyway flyway;

	@Test
	void healthIsPublicAfterMigrationsRun() {
		ResponseEntity<String> response = new TestRestTemplate()
				.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).contains("\"status\":\"UP\"");
	}
}
