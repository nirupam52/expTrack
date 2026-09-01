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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"spring.datasource.url=jdbc:sqlite::memory:", "server.servlet.session.cookie.secure=false", "exptrack.auth.max-attempts=100"})
class RegistrationEndpointTest {

	@LocalServerPort
	private int port;

	private HttpClient browser = newBrowser();
	private final ObjectMapper json = new ObjectMapper();
	@Autowired
	private JdbcTemplate jdbc;


	@Test
	void visitorCanRegisterAnAccountWithValidDetails() throws Exception {
		assertThat(register("ava@example.com", "correct-horse-battery-staple", "USD").statusCode()).isEqualTo(HttpStatus.CREATED.value());
		browser = newBrowser();
		assertThat(register("ava@example.com", "correct-horse-battery-staple", "USD").statusCode()).isEqualTo(HttpStatus.CREATED.value());
		assertThat(register("not-an-email", "correct-horse-battery-staple", "USD").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(register("ava@", "correct-horse-battery-staple", "USD").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(register("cam@example.com", "short", "USD").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(register("cam@example.com", "correct-horse-battery-staple", "").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(register("dan@example.com", "correct-horse-battery-staple", "invalid").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void registrationAcceptsPasswordsAtBothInclusiveCodePointBoundaries() throws Exception {
		assertThat(register("min@example.com", "a".repeat(15), "USD").statusCode()).isEqualTo(HttpStatus.CREATED.value());
		assertThat(register("max@example.com", "a".repeat(64), "USD").statusCode()).isEqualTo(HttpStatus.CREATED.value());
	}

	@Test
	void registrationRejectsPasswordsOutsideInclusiveCodePointBoundaries() throws Exception {
		assertThat(register("short-boundary@example.com", "a".repeat(14), "USD").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(register("long-boundary@example.com", "a".repeat(65), "USD").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void registrationMeasuresUnicodePasswordsInCodePoints() throws Exception {
		String emoji = "\uD83D\uDE00";

		assertThat(register("unicode-min@example.com", emoji.repeat(15), "USD").statusCode()).isEqualTo(HttpStatus.CREATED.value());
		assertThat(register("unicode-short@example.com", emoji.repeat(14), "USD").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void registrationAcceptsWhitespaceOnlyPasswordsWhenLengthIsValid() throws Exception {
		assertThat(register("whitespace@example.com", " ".repeat(15), "USD").statusCode()).isEqualTo(HttpStatus.CREATED.value());
	}

	@Test
	void registrationRequiresCsrfAndSessionRequiresAuthentication() throws Exception {
		HttpResponse<Void> registration = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/register")))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{}"))
				.build(), HttpResponse.BodyHandlers.discarding());
		HttpResponse<Void> session = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(), HttpResponse.BodyHandlers.discarding());

		assertThat(registration.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
		assertThat(session.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void registeredUserCanSignInAndReadTheirSession() throws Exception {
		register("bea@example.com", "correct-horse-battery-staple", "USD");
		HttpResponse<Void> signIn = signIn("bea@example.com", "correct-horse-battery-staple");
		HttpResponse<String> session = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(), HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> categories = browser.send(HttpRequest.newBuilder(URI.create(url("/api/categories"))).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(signIn.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
		assertThat(session.statusCode()).isEqualTo(HttpStatus.OK.value());
		JsonNode body = json.readTree(session.body());
		assertThat(body.get("email").asText()).isEqualTo("bea@example.com");
		assertThat(body.get("defaultCurrency").asText()).isEqualTo("USD");
		assertThat(categories.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(json.readTree(categories.body()).findValuesAsText("name")).containsExactly(
				"Dining", "Education", "Entertainment", "Fuel", "Gifts & Donations", "Groceries", "Healthcare", "Housing",
				"Insurance", "Other", "Personal Care", "Shopping", "Subscriptions", "Transportation", "Travel", "Utilities");
		assertThat(json.readTree(categories.body()).findValuesAsText("id")).containsExactly(
				"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16");
	}
	@Test
	void sessionForANoLongerExistingUserIsUnauthorized() throws Exception {
		register("eli@example.com", "correct-horse-battery-staple", "USD");
		signIn("eli@example.com", "correct-horse-battery-staple");
		jdbc.update("DELETE FROM users WHERE email = ?", "eli@example.com");
		HttpResponse<Void> session = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(), HttpResponse.BodyHandlers.discarding());

		assertThat(session.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}


	@Test
	void failedSignInIsGenericAndDoesNotCreateASession() throws Exception {
		register("cam@example.com", "correct-horse-battery-staple", "USD");
		HttpResponse<Void> signIn = signIn("cam@example.com", "wrong-password-value");
		HttpResponse<Void> session = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(), HttpResponse.BodyHandlers.discarding());

		assertThat(signIn.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(session.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void userCanSignOutAndInvalidateTheirSession() throws Exception {
		register("dan@example.com", "correct-horse-battery-staple", "USD");
		signIn("dan@example.com", "correct-horse-battery-staple");
		HttpResponse<Void> signOut = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/logout")))
				.header("X-CSRF-TOKEN", csrfToken())
				.POST(HttpRequest.BodyPublishers.noBody())
				.build(), HttpResponse.BodyHandlers.discarding());
		HttpResponse<Void> session = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(), HttpResponse.BodyHandlers.discarding());

		assertThat(signOut.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
		assertThat(session.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

	private HttpResponse<Void> register(String email, String password, String currency) throws Exception {
		return browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/register")))
				.header("Content-Type", "application/json")
				.header("X-CSRF-TOKEN", csrfToken())
				.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email", email, "password", password, "defaultCurrency", currency))))
				.build(), HttpResponse.BodyHandlers.discarding());
	}

	private HttpResponse<Void> signIn(String email, String password) throws Exception {
		return browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/login")))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("X-CSRF-TOKEN", csrfToken())
				.POST(HttpRequest.BodyPublishers.ofString("username=" + email.replace("@", "%40") + "&password=" + password))
				.build(), HttpResponse.BodyHandlers.discarding());
	}

	private String csrfToken() throws Exception {
		HttpResponse<String> response = browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/csrf"))).GET().build(), HttpResponse.BodyHandlers.ofString());
		return json.readTree(response.body()).get("token").asText();
	}

	private HttpClient newBrowser() {
		return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}
}
