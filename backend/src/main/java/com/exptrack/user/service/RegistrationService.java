package com.exptrack.user.service;

import java.util.Currency;
import java.util.Locale;

import com.exptrack.user.dto.RegistrationRequest;
import com.exptrack.user.entity.UserAccount;
import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;

	public RegistrationService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	public void register(RegistrationRequest request) {
		String email = normalizeEmail(request.email());
		String currency = currency(request.defaultCurrency());
		String password = password(request.password());
		if (users.existsByEmailIgnoreCase(email)) {
			return;
		}
		try {
			users.saveAndFlush(new UserAccount(email, passwordEncoder.encode(password), currency));
		} catch (DataIntegrityViolationException exception) {
			return;
		}
	}

	private String password(String value) {
		if (!PasswordPolicy.accepts(value)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be between "
					+ PasswordPolicy.MIN_CODE_POINTS + " and " + PasswordPolicy.MAX_CODE_POINTS + " characters");
		}
		return value;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String currency(String value) {
		try {
			return Currency.getInstance(value).getCurrencyCode();
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is invalid");
		}
	}
}
