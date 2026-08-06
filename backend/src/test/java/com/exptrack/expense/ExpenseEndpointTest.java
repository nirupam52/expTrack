package com.exptrack.expense;

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
class ExpenseEndpointTest {

	@LocalServerPort
	private int port;

	private final HttpClient browser = newBrowser();
	private final ObjectMapper json = new ObjectMapper();

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void signedInUserCanAddAnExactExpenseAndSeeOnlyTheirRecentExpenses() throws Exception {
		registerAndSignIn("ava@example.com", "USD");
		HttpResponse<String> created = create(Map.of("title", "Coffee", "amount", "92233720368547758.07", "categoryId", 1, "date", "2026-08-04", "note", "With Sam"));
		HttpResponse<String> recent = browser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses"))).GET().build(), HttpResponse.BodyHandlers.ofString());
		HttpClient otherBrowser = newBrowser();
		registerAndSignIn(otherBrowser, "bea@example.com", "USD");
		HttpResponse<String> otherRecent = otherBrowser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses"))).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(created.statusCode()).isEqualTo(HttpStatus.CREATED.value());
		JsonNode createdExpense = json.readTree(created.body());
		assertThat(createdExpense.get("amountMinor").isTextual()).isTrue();
		assertThat(createdExpense.get("amountMinor").asText()).isEqualTo("9223372036854775807");
		assertThat(createdExpense.get("categoryId").asInt()).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT category_id FROM expenses WHERE title = ?", Integer.class, "Coffee")).isEqualTo(1);
		assertThat(recent.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(json.readTree(recent.body()).get(0).get("title").asText()).isEqualTo("Coffee");
		assertThat(json.readTree(otherRecent.body())).isEmpty();
	}

	@Test
	void expenseRequiresAuthenticationCsrfValidDetailsAndCurrencyPrecision() throws Exception {
		assertThat(create(Map.of("title", "Coffee", "amount", "12.34", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		registerAndSignIn("bea@example.com", "USD");
		assertThat(withoutCsrf(Map.of("title", "Coffee", "amount", "12.34", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "12.345", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "12.34", "categoryId", 99, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	private void registerAndSignIn(String email, String currency) throws Exception {
		registerAndSignIn(browser, email, currency);
	}

	private void registerAndSignIn(HttpClient client, String email, String currency) throws Exception {
		post(client, "/api/auth/register", Map.of("email", email, "password", "correct-horse-battery-staple", "defaultCurrency", currency));
		HttpResponse<Void> signIn = client.send(HttpRequest.newBuilder(URI.create(url("/api/auth/login")))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("X-CSRF-TOKEN", csrfToken(client))
				.POST(HttpRequest.BodyPublishers.ofString("username=" + email.replace("@", "%40") + "&password=correct-horse-battery-staple"))
				.build(), HttpResponse.BodyHandlers.discarding());
		assertThat(signIn.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	private HttpResponse<String> create(Map<String, ?> expense) throws Exception {
		return post("/api/expenses", expense);
	}

	private HttpResponse<String> withoutCsrf(Map<String, ?> expense) throws Exception {
		return browser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses")))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(expense)))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String path, Map<String, ?> body) throws Exception {
		return post(browser, path, body);
	}

	private HttpResponse<String> post(HttpClient client, String path, Map<String, ?> body) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url(path)))
				.header("Content-Type", "application/json")
				.header("X-CSRF-TOKEN", csrfToken(client))
				.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	private String csrfToken() throws Exception {
		return csrfToken(browser);
	}

	private String csrfToken(HttpClient client) throws Exception {
		HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url("/api/auth/csrf"))).GET().build(), HttpResponse.BodyHandlers.ofString());
		return json.readTree(response.body()).get("token").asText();
	}

	private HttpClient newBrowser() {
		return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}
}
