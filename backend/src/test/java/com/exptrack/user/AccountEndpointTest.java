package com.exptrack.user;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"spring.datasource.url=jdbc:sqlite::memory:", "server.servlet.session.cookie.secure=false", "exptrack.auth.max-attempts=100"})
class AccountEndpointTest {

	private static final String PASSWORD = "correct-horse-battery-staple";

	private final int port;
	private final HttpClient browser;
	private final ObjectMapper json = new ObjectMapper();
	private final JdbcTemplate jdbc;
	private final BCryptPasswordEncoder legacyPasswords = new BCryptPasswordEncoder();

	@Autowired
	AccountEndpointTest(@LocalServerPort int port, JdbcTemplate jdbc) {
		this.port = port;
		this.browser = newBrowser();
		this.jdbc = jdbc;
	}

	@Test
	void sessionShowsAccountCreationTimeForNewAccountsAndNothingForLegacyAccounts() throws Exception {
		registerAndSignIn(browser, "new@example.com");
		JsonNode fresh = session(browser);

		assertThat(fresh.get("email").asText()).isEqualTo("new@example.com");
		assertThat(fresh.get("defaultCurrency").asText()).isEqualTo("USD");
		assertThat(Instant.parse(fresh.get("createdAt").asText())).isCloseTo(Instant.now(), within(5, ChronoUnit.MINUTES));
		assertThat(fresh.size()).isEqualTo(3);

		jdbc.update("INSERT INTO users (email, password_hash, default_currency) VALUES (?, ?, ?)",
				"legacy@example.com", legacyPasswords.encode(PASSWORD), "GBP");
		HttpClient legacy = newBrowser();
		signIn(legacy, "legacy@example.com", PASSWORD);
		JsonNode legacySession = session(legacy);

		assertThat(legacySession.get("email").asText()).isEqualTo("legacy@example.com");
		assertThat(legacySession.get("defaultCurrency").asText()).isEqualTo("GBP");
		assertThat(legacySession.get("createdAt").isNull()).isTrue();
	}

	@Test
	void defaultCurrencyUpdateValidatesTheCurrencyAndPersistsTheSavedValue() throws Exception {
		registerAndSignIn(browser, "cur@example.com");
		assertCurrencyUpdate();
		assertThat(session(browser).get("defaultCurrency").asText()).isEqualTo("EUR");
		assertInvalidCurrency();
		assertMissingCsrfRejected();
	}

	private void assertCurrencyUpdate() throws Exception {
		HttpResponse<String> updated = put(browser, "/api/account/default-currency", Map.of("defaultCurrency", "EUR"));
		assertThat(updated.statusCode()).isEqualTo(HttpStatus.OK.value());
		JsonNode body = json.readTree(updated.body());
		assertThat(body.get("email").asText()).isEqualTo("cur@example.com");
		assertThat(body.get("defaultCurrency").asText()).isEqualTo("EUR");
		assertThat(body.get("createdAt").isNull()).isFalse();
		assertThat(body.size()).isEqualTo(3);
	}

	private void assertInvalidCurrency() throws Exception {
		HttpResponse<String> invalid = put(browser, "/api/account/default-currency", Map.of("defaultCurrency", "not-a-currency"));
		assertThat(invalid.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(errorText(invalid)).contains("Currency");

		HttpResponse<String> blank = put(browser, "/api/account/default-currency", Map.of("defaultCurrency", ""));
		assertThat(blank.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	private void assertMissingCsrfRejected() throws Exception {
		HttpResponse<String> withoutCsrf = browser.send(HttpRequest.newBuilder(URI.create(url("/api/account/default-currency")))
				.header("Content-Type", "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("defaultCurrency", "CHF"))))
				.build(), HttpResponse.BodyHandlers.ofString());
		assertThat(withoutCsrf.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void passwordChangeRejectsWrongCurrentPasswordConfirmationMismatchAndOutOfRangePasswords() throws Exception {
		registerAndSignIn(browser, "guard@example.com");

		HttpResponse<String> wrongCurrent = password("not-the-current-password", "a-brand-new-password", "a-brand-new-password");
		assertThat(wrongCurrent.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(errorText(wrongCurrent)).contains("Current password");

		HttpResponse<String> mismatch = password(PASSWORD, "a-brand-new-password", "different-confirmation");
		assertThat(mismatch.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(errorText(mismatch)).contains("confirmation");

		assertThat(password(PASSWORD, "a".repeat(14), "a".repeat(14)).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(password(PASSWORD, "a".repeat(65), "a".repeat(65)).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(password(PASSWORD, "\uD83D\uDE00".repeat(8), "\uD83D\uDE00".repeat(8)).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

		assertThat(signIn(browser, "guard@example.com", PASSWORD).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	@Test
	void passwordChangeAcceptsFifteenCodePointPasswords() throws Exception {
		registerAndSignIn(browser, "minimum@example.com");
		String newPassword = "a".repeat(15);

		assertThat(password(PASSWORD, newPassword, newPassword).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	@Test
	void passwordChangeAcceptsSixtyFourCodePointPasswords() throws Exception {
		registerAndSignIn(browser, "maximum@example.com");
		String newPassword = "a".repeat(64);

		assertThat(password(PASSWORD, newPassword, newPassword).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}
	@Test
	void passwordChangeAcceptsWhitespaceOnlyPasswordsWhenLengthIsValid() throws Exception {
		registerAndSignIn(browser, "whitespace-password@example.com");
		String newPassword = " ".repeat(15);

		assertThat(password(PASSWORD, newPassword, newPassword).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}
	@Test
	void passwordChangeAcceptsWhitespaceOnlyCurrentPassword() throws Exception {
		String currentPassword = " ".repeat(15);
		registerAndSignIn(browser, "whitespace-current@example.com", currentPassword);
		String newPassword = "a".repeat(15);

		assertThat(password(currentPassword, newPassword, newPassword).statusCode())
				.isEqualTo(HttpStatus.NO_CONTENT.value());
	}



	@Test
	void passwordChangeMeasuresUnicodePasswordsInCodePoints() throws Exception {
		registerAndSignIn(browser, "unicode-boundary@example.com");
		String emoji = "\uD83D\uDE00";
		String validPassword = emoji.repeat(15);
		String shortPassword = emoji.repeat(14);

		assertThat(password(PASSWORD, shortPassword, shortPassword).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(password(PASSWORD, validPassword, validPassword).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

		HttpClient fresh = newBrowser();
		assertThat(signIn(fresh, "unicode-boundary@example.com", validPassword).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}



	@Test
	void passwordChangeAcceptsUnicodePasswordsMeasuredInCodePoints() throws Exception {
		registerAndSignIn(browser, "unicode@example.com");

		String unicode = "\uD83D\uDE00".repeat(40) + "a".repeat(20);
		HttpResponse<String> change = password(PASSWORD, unicode, unicode);
		assertThat(change.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

		HttpClient fresh = newBrowser();
		assertThat(signIn(fresh, "unicode@example.com", unicode).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
		assertThat(signIn(newBrowser(), "unicode@example.com", PASSWORD).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void passwordChangeEndsEveryActiveSessionForTheUserButNotOtherUsers() throws Exception {
		registerAndSignIn(browser, "keeper@example.com");
		HttpClient second = newBrowser();
		signIn(second, "keeper@example.com", PASSWORD);
		assertThat(session(second).get("email").asText()).isEqualTo("keeper@example.com");
		HttpClient otherUser = newBrowser();
		registerAndSignIn(otherUser, "other-user@example.com");

		String newPassword = "a-brand-new-password";
		assertThat(password(PASSWORD, newPassword, newPassword).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

		assertThat(browser.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(),
				HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(second.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(),
				HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(session(otherUser).get("email").asText()).isEqualTo("other-user@example.com");

		assertThat(signIn(newBrowser(), "keeper@example.com", newPassword).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
		assertThat(signIn(newBrowser(), "keeper@example.com", PASSWORD).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void passwordChangeAllowsSameBrowserToSignInWithNewPassword() throws Exception {
		registerAndSignIn(browser, "same-browser@example.com");
		String newPassword = "a-brand-new-password";

		assertThat(password(PASSWORD, newPassword, newPassword).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
		assertThat(signIn(browser, "same-browser@example.com", newPassword).statusCode())
				.isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	private String errorText(HttpResponse<String> response) throws Exception {
		JsonNode body = json.readTree(response.body());
		return body.path("detail").asText(body.path("message").asText());
	}

	private JsonNode session(HttpClient client) throws Exception {
		HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url("/api/auth/session"))).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		return json.readTree(response.body());
	}

	private HttpResponse<String> password(String current, String newPassword, String confirmation) throws Exception {
		return post(browser, "/api/account/password",
				Map.of("currentPassword", current, "newPassword", newPassword, "newPasswordConfirmation", confirmation));
	}

	private void registerAndSignIn(HttpClient client, String email) throws Exception {
		registerAndSignIn(client, email, PASSWORD);
	}

	private void registerAndSignIn(HttpClient client, String email, String password) throws Exception {
		HttpResponse<Void> registration = postWithoutBody(client, "/api/auth/register",
				Map.of("email", email, "password", password, "defaultCurrency", "USD"));
		assertThat(registration.statusCode()).isEqualTo(HttpStatus.CREATED.value());
		assertThat(signIn(client, email, password).statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	private HttpResponse<Void> signIn(HttpClient client, String email, String password) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url("/api/auth/login")))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("X-CSRF-TOKEN", csrfToken(client))
				.POST(HttpRequest.BodyPublishers.ofString("username=" + email.replace("@", "%40") + "&password=" + password))
				.build(), HttpResponse.BodyHandlers.discarding());
	}

	private HttpResponse<String> put(HttpClient client, String path, Map<String, ?> body) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url(path)))
				.header("Content-Type", "application/json")
				.header("X-CSRF-TOKEN", csrfToken(client))
				.PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<Void> postWithoutBody(HttpClient client, String path, Map<String, ?> body) throws Exception {
		return client.send(postRequest(client, path, body), HttpResponse.BodyHandlers.discarding());
	}

	private HttpResponse<String> post(HttpClient client, String path, Map<String, ?> body) throws Exception {
		return client.send(postRequest(client, path, body), HttpResponse.BodyHandlers.ofString());
	}

	private HttpRequest postRequest(HttpClient client, String path, Map<String, ?> body) throws Exception {
		return HttpRequest.newBuilder(URI.create(url(path)))
				.header("Content-Type", "application/json")
				.header("X-CSRF-TOKEN", csrfToken(client))
				.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
				.build();
	}

	private String csrfToken(HttpClient client) throws Exception {
		HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url("/api/auth/csrf"))).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		return json.readTree(response.body()).get("token").asText();
	}

	private HttpClient newBrowser() {
		return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}
}
