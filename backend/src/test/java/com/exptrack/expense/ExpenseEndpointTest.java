package com.exptrack.expense;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
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
	void dashboardGroupsCurrentMonthTotalsByRecordedCurrencyAndReturnsTheLatestFiveExpenses() throws Exception {
		registerAndSignIn("dashboard-ava@example.com", "USD");
		YearMonth currentMonth = YearMonth.now();
		LocalDate first = currentMonth.atDay(1);
		create(Map.of("title", "Groceries", "amount", "80.00", "categoryId", 1, "date", first.plusDays(1).toString()));
		create(Map.of("title", "Train pass", "amount", "50.00", "categoryId", 2, "date", first.plusDays(2).toString()));
		jdbc.update("UPDATE users SET default_currency = ? WHERE email = ?", "EUR", "dashboard-ava@example.com");
		create(Map.of("title", "Museum", "amount", "12.00", "categoryId", 3, "date", first.plusDays(3).toString()));
		create(Map.of("title", "Coffee", "amount", "4.50", "categoryId", 1, "date", first.plusDays(4).toString()));
		create(Map.of("title", "Dinner", "amount", "25.00", "categoryId", 2, "date", first.plusDays(5).toString()));
		create(Map.of("title", "Market", "amount", "28.00", "categoryId", 1, "date", first.plusDays(6).toString()));
		create(Map.of("title", "Old expense", "amount", "999.00", "categoryId", 1, "date", first.minusDays(1).toString()));
		HttpClient otherBrowser = newBrowser();
		registerAndSignIn(otherBrowser, "dashboard-bea@example.com", "USD");
		post(otherBrowser, "/api/expenses", Map.of("title", "Private expense", "amount", "300.00", "categoryId", 1, "date", first.plusDays(7).toString()));
		HttpResponse<String> dashboard = browser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses/dashboard"))).GET().build(), HttpResponse.BodyHandlers.ofString());
		JsonNode body = json.readTree(dashboard.body());

		assertThat(dashboard.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(body.get("month").asText()).isEqualTo(currentMonth.toString());
		assertThat(body.get("currencies")).extracting(node -> node.get("currency").asText()).containsExactly("EUR", "USD");
		assertThat(body.get("currencies").get(0).get("totalMinor").asText()).isEqualTo("6950");
		assertThat(body.get("currencies").get(0).get("categories")).extracting(node -> node.get("categoryId").asInt()).containsExactly(1, 2, 3);
		assertThat(body.get("currencies").get(0).get("categories")).extracting(node -> node.get("amountMinor").asText()).containsExactly("3250", "2500", "1200");
		assertThat(body.get("currencies").get(1).get("totalMinor").asText()).isEqualTo("13000");
		assertThat(body.get("currencies").get(1).get("categories")).extracting(node -> node.get("categoryId").asInt()).containsExactly(1, 2);
		assertThat(body.get("currencies").get(1).get("categories")).extracting(node -> node.get("amountMinor").asText()).containsExactly("8000", "5000");
		assertThat(body.get("currencies").get(1).get("categories").get(0).get("amountMinor").asText()).isEqualTo("8000");
		assertThat(body.get("recentExpenses")).hasSize(5);
		assertThat(body.get("recentExpenses").get(0).get("title").asText()).isEqualTo("Market");
		assertThat(body.get("recentExpenses").get(4).get("title").asText()).isEqualTo("Train pass");
		assertThat(body.get("recentExpenses")).extracting(node -> node.get("title").asText()).doesNotContain("Private expense");
	}

	@Test
	void dashboardKeepsTotalsExactBeyondTheLongRange() throws Exception {
		registerAndSignIn("dashboard-overflow@example.com", "USD");
		String date = YearMonth.now().atDay(1).toString();
		Map<String, Object> expense = Map.of("title", "Large expense", "amount", "92233720368547758.07", "categoryId", 1, "date", date);
		create(expense);
		create(expense);
		JsonNode body = json.readTree(browser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses/dashboard"))).GET().build(), HttpResponse.BodyHandlers.ofString()).body());

		assertThat(body.get("currencies").get(0).get("totalMinor").asText()).isEqualTo("18446744073709551614");
		assertThat(body.get("currencies").get(0).get("categories").get(0).get("amountMinor").asText()).isEqualTo("18446744073709551614");
	}

	@Test
	void dashboardHasNoCurrencySummaryWithoutCurrentMonthExpensesAndOrdersEqualDatesById() throws Exception {
		registerAndSignIn("dashboard-empty@example.com", "USD");
		String date = YearMonth.now().atDay(1).toString();
		create(Map.of("title", "First", "amount", "1.00", "categoryId", 1, "date", date));
		create(Map.of("title", "Second", "amount", "1.00", "categoryId", 1, "date", date));
		JsonNode populated = json.readTree(browser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses/dashboard"))).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
		assertThat(populated.get("recentExpenses")).extracting(node -> node.get("title").asText()).containsExactly("Second", "First");

		HttpClient emptyBrowser = newBrowser();
		registerAndSignIn(emptyBrowser, "dashboard-no-current-month@example.com", "USD");
		post(emptyBrowser, "/api/expenses", Map.of("title", "Old expense", "amount", "1.00", "categoryId", 1, "date", YearMonth.now().minusMonths(1).atDay(1).toString()));
		JsonNode empty = json.readTree(emptyBrowser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses/dashboard"))).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
		assertThat(empty.get("currencies")).isEmpty();
		assertThat(empty.get("recentExpenses").get(0).get("title").asText()).isEqualTo("Old expense");
	}

	@Test
	void signedInUserCanAddAnExactExpenseAndSeeOnlyTheirRecentExpenses() throws Exception {
		registerAndSignIn("expense-ava@example.com", "USD");
		HttpResponse<String> created = create(Map.of("title", "Coffee", "amount", "92233720368547758.07", "categoryId", 1, "date", "2026-08-04", "note", "With Sam"));
		HttpResponse<String> recent = browser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses"))).GET().build(), HttpResponse.BodyHandlers.ofString());
		HttpClient otherBrowser = newBrowser();
		registerAndSignIn(otherBrowser, "expense-bea@example.com", "USD");
		HttpResponse<String> otherRecent = otherBrowser.send(HttpRequest.newBuilder(URI.create(url("/api/expenses"))).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(created.statusCode()).isEqualTo(HttpStatus.CREATED.value());
		JsonNode createdExpense = json.readTree(created.body());
		assertThat(createdExpense.get("amountMinor").isTextual()).isTrue();
		assertThat(createdExpense.get("amountMinor").asText()).isEqualTo("9223372036854775807");
		assertThat(createdExpense.get("categoryId").asInt()).isEqualTo(1);
		assertThat(createdExpense.get("currency").asText()).isEqualTo("USD");
		assertThat(jdbc.queryForObject("SELECT category_id FROM expenses WHERE title = ?", Integer.class, "Coffee")).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT currency FROM expenses WHERE title = ?", String.class, "Coffee")).isEqualTo("USD");
		assertThat(recent.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(json.readTree(recent.body()).get("items").get(0).get("title").asText()).isEqualTo("Coffee");
		assertThat(json.readTree(otherRecent.body()).get("items")).isEmpty();
	}

	@Test
	void expenseRejectsMissingOrBlankRequiredDetailsAndNonPositiveAmounts() throws Exception {
		assertThat(create(Map.of("title", "Coffee", "amount", "12.34", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		registerAndSignIn("expense-validation@example.com", "USD");

		assertThat(withoutCsrf(Map.of("title", "Coffee", "amount", "12.34", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
		assertThat(create(Map.of("title", "", "amount", "12.34", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", " ", "amount", "12.34", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "12.34", "categoryId", 1)).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "12.34", "categoryId", 1, "date", "")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "0", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "-1", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "12.345", "categoryId", 1, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(create(Map.of("title", "Coffee", "amount", "12.34", "categoryId", 99, "date", "2026-08-04")).statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void expenseSupportsThreeDecimalBhdAndOmitsOptionalNotes() throws Exception {
		registerAndSignIn("expense-bhd@example.com", "BHD");
		HttpResponse<String> bhd = create(Map.of("title", "Lunch", "amount", "12.345", "categoryId", 1, "date", "2026-08-04"));
		HttpResponse<String> invalidBhd = create(Map.of("title", "Lunch", "amount", "12.3456", "categoryId", 1, "date", "2026-08-04"));

		JsonNode bhdExpense = json.readTree(bhd.body());
		assertThat(bhd.statusCode()).isEqualTo(HttpStatus.CREATED.value());
		assertThat(bhdExpense.get("amountMinor").asText()).isEqualTo("12345");
		assertThat(bhdExpense.get("currency").asText()).isEqualTo("BHD");
		assertThat(bhdExpense.get("note").isNull()).isTrue();
		assertThat(jdbc.queryForObject("SELECT currency FROM expenses WHERE title = ?", String.class, "Lunch")).isEqualTo("BHD");
		assertThat(invalidBhd.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void expenseSupportsWholeUnitJpyAndKeepsOptionalNotes() throws Exception {
		HttpClient jpyBrowser = newBrowser();
		registerAndSignIn(jpyBrowser, "expense-jpy@example.com", "JPY");
		HttpResponse<String> jpy = post(jpyBrowser, "/api/expenses", Map.of("title", "Train", "amount", "123", "categoryId", 1, "date", "2026-08-04", "note", "Train fare"));
		HttpResponse<String> invalidJpy = post(jpyBrowser, "/api/expenses", Map.of("title", "Train", "amount", "123.1", "categoryId", 1, "date", "2026-08-04"));

		JsonNode jpyExpense = json.readTree(jpy.body());
		assertThat(jpy.statusCode()).isEqualTo(HttpStatus.CREATED.value());
		assertThat(jpyExpense.get("amountMinor").asText()).isEqualTo("123");
		assertThat(jpyExpense.get("currency").asText()).isEqualTo("JPY");
		assertThat(jpyExpense.get("note").asText()).isEqualTo("Train fare");
		assertThat(jdbc.queryForObject("SELECT note FROM expenses WHERE title = ?", String.class, "Train")).isEqualTo("Train fare");
		assertThat(invalidJpy.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	private void registerAndSignIn(String email, String currency) throws Exception {
		registerAndSignIn(browser, email, currency);
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
