package com.exptrack.user;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.datasource.url=jdbc:sqlite::memory:")
class RegistrationEndpointTest {

	@LocalServerPort
	private int port;

	private final HttpClient browser = HttpClient.newBuilder()
			.cookieHandler(new CookieManager())
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();
	private final ObjectMapper json = new ObjectMapper();

	@Test
	void visitorCanRegisterAnAccount() {
		ResponseEntity<Void> response = new TestRestTemplate().postForEntity(
				"http://localhost:" + port + "/api/auth/register",
				Map.of("email", "ava@example.com", "password", "correct-horse-battery-staple", "defaultCurrency", "USD"),
				Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

	@Test
	void registeredUserCanSignInAndReadTheirSession() throws Exception {
		browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/register")))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"bea@example.com\",\"password\":\"correct-horse-battery-staple\",\"defaultCurrency\":\"USD\"}"))
				.build(), HttpResponse.BodyHandlers.discarding());
		HttpResponse<String> csrf = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/csrf"))).GET().build(), HttpResponse.BodyHandlers.ofString());
		String token = json.readTree(csrf.body()).get("token").asText();
		HttpResponse<Void> signIn = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/login")))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("X-XSRF-TOKEN", token)
				.POST(HttpRequest.BodyPublishers.ofString("username=bea%40example.com&password=correct-horse-battery-staple"))
				.build(), HttpResponse.BodyHandlers.discarding());
		HttpResponse<String> session = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(signIn.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
		assertThat(session.statusCode()).isEqualTo(HttpStatus.OK.value());
		JsonNode body = json.readTree(session.body());
		assertThat(body.get("email").asText()).isEqualTo("bea@example.com");
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}
}
