package com.exptrack.user.service;

import java.util.Currency;
import java.util.List;
import java.util.Locale;

import com.exptrack.category.entity.Category;
import com.exptrack.category.repository.CategoryRepository;
import com.exptrack.user.dto.RegistrationRequest;
import com.exptrack.user.entity.UserAccount;
import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

	private final UserAccountRepository users;
	private final CategoryRepository categories;
	private final PasswordEncoder passwordEncoder;

	public RegistrationService(UserAccountRepository users, CategoryRepository categories, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.categories = categories;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void register(RegistrationRequest request) {
		String email = normalizeEmail(request.email());
		String currency = currency(request.defaultCurrency());
		if (request.password() == null || request.password().length() < 12) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 12 characters");
		}
		if (users.existsByEmailIgnoreCase(email)) {
			return;
		}
		try {
			UserAccount user = users.saveAndFlush(new UserAccount(email, passwordEncoder.encode(request.password()), currency));
			categories.saveAll(List.of("Restaurants", "Food", "Gas", "Groceries", "Entertainment").stream()
					.map(name -> new Category(user.getId(), name))
					.toList());
		} catch (DataIntegrityViolationException exception) {
			return;
		}
	}

	private String normalizeEmail(String email) {
		if (email == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
		}
		String normalized = email.trim().toLowerCase(Locale.ROOT);
		if (normalized.length() > 254 || !normalized.contains("@")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is invalid");
		}
		return normalized;
	}

	private String currency(String value) {
		try {
			return Currency.getInstance(value).getCurrencyCode();
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is invalid");
		}
	}
}
