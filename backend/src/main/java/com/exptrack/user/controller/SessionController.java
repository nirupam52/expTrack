package com.exptrack.user.controller;

import java.security.Principal;
import java.util.Map;

import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SessionController {

	private final UserAccountRepository users;

	public SessionController(UserAccountRepository users) {
		this.users = users;
	}

	@GetMapping("/csrf")
	Map<String, String> csrf(@RequestAttribute("_csrf") CsrfToken token) {
		return Map.of("token", token.getToken());
	}

	@GetMapping("/session")
	Map<String, String> session(Principal principal) {
		return users.findByEmailIgnoreCase(principal.getName())
				.map(user -> Map.of("email", user.getEmail(), "defaultCurrency", user.getDefaultCurrency()))
				.orElseThrow();
	}
}
