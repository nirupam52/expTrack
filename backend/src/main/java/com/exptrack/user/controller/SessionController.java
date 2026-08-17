package com.exptrack.user.controller;

import java.security.Principal;
import java.util.Map;

import com.exptrack.user.dto.SessionResponse;
import com.exptrack.user.service.SessionService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SessionController {

	private final SessionService sessions;

	public SessionController(SessionService sessions) {
		this.sessions = sessions;
	}

	@GetMapping("/csrf")
	Map<String, String> csrf(@RequestAttribute("_csrf") CsrfToken token) {
		return Map.of("token", token.getToken());
	}

	@GetMapping("/session")
	SessionResponse session(Principal principal) {
		return sessions.current(principal.getName());
	}
}
