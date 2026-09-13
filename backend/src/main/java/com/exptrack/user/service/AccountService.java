package com.exptrack.user.service;

import java.util.Objects;

import com.exptrack.currency.service.CurrencyService;
import com.exptrack.user.dto.ChangePasswordRequest;
import com.exptrack.user.dto.SessionResponse;
import com.exptrack.user.entity.UserAccount;
import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

	private final UserAccountRepository users;
	private final PasswordEncoder passwords;
	private final SessionRegistry sessions;
	private final CurrencyService currencies;

	public AccountService(UserAccountRepository users, PasswordEncoder passwords, SessionRegistry sessions,
			CurrencyService currencies) {
		this.users = users;
		this.passwords = passwords;
		this.sessions = sessions;
		this.currencies = currencies;
	}

	@Transactional
	public SessionResponse updateDefaultCurrency(String email, String requestedCurrency) {
		UserAccount user = find(email);
		user.setDefaultCurrency(currencies.validate(requestedCurrency));
		return new SessionResponse(user.getEmail(), user.getDefaultCurrency(), user.getCreatedAt());
	}

	@Transactional
	public void changePassword(String email, ChangePasswordRequest request) {
		UserAccount user = find(email);
		if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
			throw badRequest("Current password is incorrect");
		}
		if (!Objects.equals(request.newPassword(), request.newPasswordConfirmation())) {
			throw badRequest("New password and confirmation do not match");
		}
		if (!PasswordPolicy.accepts(request.newPassword())) {
			throw badRequest("New password must be between " + PasswordPolicy.MIN_CODE_POINTS + " and "
					+ PasswordPolicy.MAX_CODE_POINTS + " characters");
		}
		user.setPasswordHash(passwords.encode(request.newPassword()));
		sessions.getAllSessions(email, false).forEach(SessionInformation::expireNow);
	}

	private UserAccount find(String email) {
		return users.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	private ErrorResponseException badRequest(String detail) {
		return new ErrorResponseException(HttpStatus.BAD_REQUEST,
				ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), null);
	}
}
