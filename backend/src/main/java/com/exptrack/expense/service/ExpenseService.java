package com.exptrack.expense.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exptrack.category.service.CategoryService;
import com.exptrack.expense.dto.DashboardCategoryResponse;
import com.exptrack.expense.dto.DashboardCurrencyResponse;
import com.exptrack.expense.dto.DashboardResponse;
import com.exptrack.expense.dto.ExpenseHistoryRequest;
import com.exptrack.expense.dto.ExpensePageResponse;
import com.exptrack.expense.dto.ExpenseRequest;
import com.exptrack.expense.dto.ExpenseResponse;
import com.exptrack.expense.entity.Expense;
import com.exptrack.expense.entity.ExpenseDetails;
import com.exptrack.expense.repository.ExpenseRepository;
import com.exptrack.expense.repository.DashboardCategoryTotal;
import com.exptrack.expense.repository.ExpenseHistoryQuery;
import com.exptrack.user.entity.UserAccount;
import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExpenseService {

	private final ExpenseRepository expenses;
	private final UserAccountRepository users;
	private final CategoryService categories;

	public ExpenseService(ExpenseRepository expenses, UserAccountRepository users, CategoryService categories) {
		this.expenses = expenses;
		this.users = users;
		this.categories = categories;
	}

	public ExpenseResponse create(ExpenseRequest request, String email) {
		UserAccount user = currentUser(email);
		Expense expense = expenses.save(new Expense(user.getId(), details(request, user.getDefaultCurrency())));
		return response(expense);
	}

	public ExpensePageResponse history(ExpenseHistoryRequest request, String email) {
		int limit = request.limit() == null ? 20 : request.limit();
		validateHistoryFilters(request.categoryId(), request.from(), request.to());
		Cursor cursor = decodeCursor(request.cursor());
		ExpenseHistoryQuery history = new ExpenseHistoryQuery(
				currentUser(email).getId(), searchPhrase(request.query()), request.categoryId(), request.from(), request.to(),
				cursor.date(), cursor.id(), limit + 1);
		List<Expense> matches = expenses.findHistory(history);
		boolean hasMore = matches.size() > limit;
		List<ExpenseResponse> items = matches.stream().limit(limit).map(this::response).toList();
		return new ExpensePageResponse(items, hasMore ? encodeCursor(matches.get(limit - 1)) : null);
	}

	public DashboardResponse dashboard(String email) {
		UserAccount user = currentUser(email);
		YearMonth month = YearMonth.now();
		Map<String, List<DashboardCategoryTotal>> totalsByCurrency = new LinkedHashMap<>();
		for (DashboardCategoryTotal total : expenses.findDashboardCategoryTotals(user.getId(), month.atDay(1), month.plusMonths(1).atDay(1))) {
			totalsByCurrency.computeIfAbsent(total.getCurrency(), ignored -> new ArrayList<>()).add(total);
		}
		List<DashboardCurrencyResponse> currencies = totalsByCurrency.entrySet().stream().map(this::currencyResponse).toList();
		List<ExpenseResponse> recentExpenses = expenses.findTop5ByUserIdOrderByExpenseDateDescIdDesc(user.getId()).stream().map(this::response).toList();
		return new DashboardResponse(month.toString(), currencies, recentExpenses);
	}

	@Transactional
	public ExpenseResponse update(Integer expenseId, ExpenseRequest request, String email) {
		Expense expense = ownedExpense(expenseId, email);
		expense.update(details(request, expense.getCurrency()));
		return response(expense);
	}

	@Transactional
	public void delete(Integer expenseId, String email) {
		expenses.delete(ownedExpense(expenseId, email));
	}

	private ExpenseDetails details(ExpenseRequest request, String currency) {
		if (!categories.isValid(request.categoryId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is invalid");
		}
		return new ExpenseDetails(
				request.title().trim(),
				amountMinor(request.amount(), currency),
				request.categoryId(),
				request.date(),
				currency,
				optionalText(request.note()));
	}

	private void validateHistoryFilters(Integer categoryId, LocalDate from, LocalDate to) {
		if (categoryId != null && !categories.isValid(categoryId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is invalid");
		}
		if (from != null && to != null && from.isAfter(to)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range is invalid");
		}
	}

	private Cursor decodeCursor(String encodedCursor) {
		if (encodedCursor == null) return Cursor.empty();
		try {
			String[] values = new String(Base64.getUrlDecoder().decode(encodedCursor)).split("\\|", -1);
			if (values.length != 2) throw new IllegalArgumentException();
			Integer id = Integer.valueOf(values[1]);
			if (id <= 0) throw new IllegalArgumentException();
			return new Cursor(LocalDate.parse(values[0]), id);
		} catch (IllegalArgumentException | DateTimeException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor is invalid");
		}
	}

	private String searchPhrase(String value) {
		String text = optionalText(value);
		if (text == null) return null;
		String terms = text.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
		return "\"" + (terms.isEmpty() ? "__exptrack_no_match__" : terms) + "\"";
	}

	private String encodeCursor(Expense expense) {
		String value = expense.getExpenseDate() + "|" + expense.getId();
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private Expense ownedExpense(Integer expenseId, String email) {
		return expenses.findByIdAndUserId(expenseId, currentUser(email).getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private record Cursor(LocalDate date, Integer id) {
		private static Cursor empty() {
			return new Cursor(null, null);
		}
	}

	private UserAccount currentUser(String email) {
		return users.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	private long amountMinor(String value, String currencyCode) {
		try {
			Currency currency = Currency.getInstance(currencyCode);
			int fractionDigits = currency.getDefaultFractionDigits();
			BigDecimal amount = new BigDecimal(value.trim());
			if (fractionDigits < 0 || amount.signum() <= 0 || !value.trim().matches("\\d+(?:\\.\\d+)?")) {
				throw new ArithmeticException();
			}
			return amount.setScale(fractionDigits, RoundingMode.UNNECESSARY).movePointRight(fractionDigits).longValueExact();
		} catch (IllegalArgumentException | ArithmeticException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is invalid");
		}
	}

	private String optionalText(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private ExpenseResponse response(Expense expense) {
		return new ExpenseResponse(expense.getId(), expense.getTitle(), Long.toString(expense.getAmountMinor()), expense.getCategoryId(),
				expense.getExpenseDate(), expense.getCurrency(), expense.getNote());
	}

	private DashboardCurrencyResponse currencyResponse(Map.Entry<String, List<DashboardCategoryTotal>> entry) {
		long total = entry.getValue().stream().mapToLong(DashboardCategoryTotal::getAmountMinor).sum();
		List<DashboardCategoryResponse> categories = entry.getValue().stream()
				.map(value -> new DashboardCategoryResponse(value.getCategoryId(), Long.toString(value.getAmountMinor()))).toList();
		return new DashboardCurrencyResponse(entry.getKey(), Long.toString(total), categories);
	}
}
