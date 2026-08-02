package com.exptrack.user;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"spring.datasource.url=jdbc:sqlite::memory:", "exptrack.auth.max-attempts=1"})
class AuthRateLimitTest {

	@LocalServerPort
	private int port;

	@Test
	void registrationAttemptsAreRateLimitedPerClient() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/register"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{}"))
				.build();

		client.send(request, HttpResponse.BodyHandlers.discarding());
		HttpResponse<Void> limited = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(limited.statusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
	}
}
