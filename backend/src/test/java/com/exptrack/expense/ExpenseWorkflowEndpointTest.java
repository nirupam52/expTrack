package com.exptrack.expense;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"spring.datasource.url=jdbc:sqlite::memory:", "server.servlet.session.cookie.secure=false", "exptrack.auth.max-attempts=100"})
class ExpenseWorkflowEndpointTest {

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcTemplate jdbc;

	private final HttpClient browser = newBrowser();
	private final ObjectMapper json = new ObjectMapper();

	@Test
	void historyFiltersByTextCategoryAndDateThenUsesCursor() throws Exception {
		registerAndSignIn(browser, "history-ava@example.com", "USD");
		post(browser, "/api/expenses", Map.of("title", "Coffee beans", "amount", "12.34", "categoryId", 1, "date", "2026-08-06", "note", "Home"));
		post(browser, "/api/expenses", Map.of("title", "Coffee catch-up", "amount", "4.50", "categoryId", 1, "date", "2026-08-04", "note", "With Sam"));
		post(browser, "/api/expenses", Map.of("title", "Restaurant", "amount", "20.00", "categoryId", 2, "date", "2026-08-07", "note", "Coffee dessert"));
		HttpClient otherBrowser = newBrowser();
		registerAndSignIn(otherBrowser, "history-bea@example.com", "USD");
		post(otherBrowser, "/api/expenses", Map.of("title", "Coffee", "amount", "2.50", "categoryId", 1, "date", "2026-08-08", "note", "Private"));

		HttpResponse<String> first = get("/api/expenses?query=coffee&categoryId=1&from=2026-08-01&to=2026-08-31&limit=1");
		JsonNode firstPage = json.readTree(first.body());
		String cursor = firstPage.get("nextCursor").asText();
		HttpResponse<String> second = get("/api/expenses?query=coffee&categoryId=1&from=2026-08-01&to=2026-08-31&limit=1&cursor=" + cursor);
		JsonNode secondPage = json.readTree(second.body());

		assertThat(first.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(firstPage.get("items").get(0).get("title").asText()).isEqualTo("Coffee beans");
		assertThat(firstPage.get("nextCursor").isTextual()).isTrue();
		assertThat(secondPage.get("items").get(0).get("title").asText()).isEqualTo("Coffee catch-up");
		assertThat(secondPage.get("nextCursor").isNull()).isTrue();
	}

	@Test
	void updatePreservesRecordedCurrencyAndDeleteIsOwnerScoped() throws Exception {
		registerAndSignIn(browser, "mutation-ava@example.com", "USD");
		HttpResponse<String> created = post(browser, "/api/expenses", Map.of("title", "Train", "amount", "9.50", "categoryId", 1, "date", "2026-08-04", "note", "Commute"));
		int expenseId = json.readTree(created.body()).get("id").asInt();
		jdbc.update("UPDATE users SET default_currency = ? WHERE email = ?", "JPY", "mutation-ava@example.com");
		HttpResponse<String> updated = put(browser, "/api/expenses/" + expenseId,
				Map.of("title", "Train pass", "amount", "10.25", "categoryId", 2, "date", "2026-08-05", "note", "Monthly"));
		JsonNode updatedExpense = json.readTree(updated.body());
		assertThat(updatedExpense.get("amountMinor").asText()).isEqualTo("1025");
		assertThat(updatedExpense.get("currency").asText()).isEqualTo("USD");
		HttpResponse<String> searchedUpdatedExpense = get("/api/expenses?query=pass");
		assertThat(searchedUpdatedExpense.statusCode()).isEqualTo(HttpStatus.OK.value());
		JsonNode searchedItems = json.readTree(searchedUpdatedExpense.body()).get("items");
		assertThat(searchedItems.get(0).get("title").asText()).isEqualTo("Train pass");

		HttpClient otherBrowser = newBrowser();
		registerAndSignIn(otherBrowser, "mutation-bea@example.com", "USD");
		HttpResponse<String> otherUpdate = put(otherBrowser, "/api/expenses/" + expenseId,
				Map.of("title", "Stolen", "amount", "1.00", "categoryId", 1, "date", "2026-08-05"));
		HttpResponse<Void> otherDelete = delete(otherBrowser, "/api/expenses/" + expenseId);
		HttpResponse<Void> deleted = delete(browser, "/api/expenses/" + expenseId);

		assertThat(otherUpdate.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(otherDelete.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(deleted.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
		assertThat(json.readTree(get("/api/expenses").body()).get("items")).isEmpty();
	}

	@Test
	void historyRejectsInvalidFiltersAndCursors() throws Exception {
		registerAndSignIn(browser, "history-validation@example.com", "USD");

		assertThat(get("/api/expenses?from=2026-08-31&to=2026-08-01").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(get("/api/expenses?categoryId=99").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(get("/api/expenses?cursor=not-a-cursor").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(get("/api/expenses?cursor=" + "a".repeat(65)).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		String invalidDateCursor = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("2026-99-01|1".getBytes(StandardCharsets.UTF_8));
		assertThat(get("/api/expenses?cursor=" + invalidDateCursor).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		String nonPositiveIdCursor = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("2026-08-01|0".getBytes(StandardCharsets.UTF_8));
		assertThat(get("/api/expenses?cursor=" + nonPositiveIdCursor).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	private void registerAndSignIn(HttpClient client, String email, String currency) throws Exception {
		HttpResponse<String> registration = post(client, "/api/auth/register",
				Map.of("email", email, "password", "correct-horse-battery-staple", "defaultCurrency", currency));
		assertThat(registration.statusCode()).isEqualTo(HttpStatus.CREATED.value());
		HttpResponse<Void> signIn = client.send(HttpRequest.newBuilder(URI.create(url("/api/auth/login")))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("X-CSRF-TOKEN", csrfToken(client))
				.POST(HttpRequest.BodyPublishers.ofString("username=" + email.replace("@", "%40") + "&password=correct-horse-battery-staple"))
				.build(), HttpResponse.BodyHandlers.discarding());
		assertThat(signIn.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	private HttpResponse<String> get(String path) throws Exception {
		return browser.send(HttpRequest.newBuilder(URI.create(url(path))).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(HttpClient client, String path, Map<String, ?> body) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url(path)))
				.header("Content-Type", "application/json")
				.header("X-CSRF-TOKEN", csrfToken(client))
				.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> put(HttpClient client, String path, Map<String, ?> body) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url(path)))
				.header("Content-Type", "application/json")
				.header("X-CSRF-TOKEN", csrfToken(client))
				.PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<Void> delete(HttpClient client, String path) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url(path)))
				.header("X-CSRF-TOKEN", csrfToken(client))
				.DELETE()
				.build(), HttpResponse.BodyHandlers.discarding());
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
