package com.exptrack.expense.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

import com.exptrack.category.service.CategoryService;
import com.exptrack.expense.dto.ExpenseRequest;
import com.exptrack.expense.dto.ExpenseResponse;
import com.exptrack.expense.entity.Expense;
import com.exptrack.expense.repository.ExpenseRepository;
import com.exptrack.user.entity.UserAccount;
import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
		if (!categories.isValid(request.categoryId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is invalid");
		}
		Expense expense = expenses.save(new Expense(user.getId(), request.title().trim(), amountMinor(request.amount(), user.getDefaultCurrency()),
				request.categoryId(), request.date(), user.getDefaultCurrency(), optionalText(request.note())));
		return response(expense);
	}

	public List<ExpenseResponse> recent(String email) {
		return expenses.findTop10ByUserIdOrderByExpenseDateDescIdDesc(currentUser(email).getId()).stream().map(this::response).toList();
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
}
