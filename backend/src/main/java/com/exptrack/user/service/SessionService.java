package com.exptrack.user.service;

import com.exptrack.expense.service.CurrencySnapshotService;
import com.exptrack.user.dto.SessionResponse;
import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SessionService {
	private final UserAccountRepository users;

	public SessionService(UserAccountRepository users) {
		this.users = users;
	}

	public SessionResponse current(String email) {
		return users.findByEmailIgnoreCase(email)
				.map(user -> new SessionResponse(user.getEmail(), user.getDefaultCurrency(), user.getCreatedAt(),
						CurrencySnapshotService.issue(user.getEmail(), user.getDefaultCurrency())))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}
}
