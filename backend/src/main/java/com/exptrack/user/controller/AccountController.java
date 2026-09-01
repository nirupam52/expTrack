package com.exptrack.user.controller;

import java.security.Principal;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import com.exptrack.user.dto.ChangePasswordRequest;
import com.exptrack.user.dto.DefaultCurrencyRequest;
import com.exptrack.user.dto.SessionResponse;
import com.exptrack.user.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

	private final AccountService accounts;

	public AccountController(AccountService accounts) {
		this.accounts = accounts;
	}

	@PutMapping("/default-currency")
	SessionResponse defaultCurrency(Principal principal, @Valid @RequestBody DefaultCurrencyRequest request) {
		return accounts.updateDefaultCurrency(principal.getName(), request.defaultCurrency());
	}

	@PostMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void password(Principal principal, HttpSession session, @Valid @RequestBody ChangePasswordRequest request) {
		accounts.changePassword(principal.getName(), request);
		accounts.expireSessions(principal.getName(), session);
	}
}
