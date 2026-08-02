package com.exptrack.user.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SessionController {

	@GetMapping("/csrf")
	Map<String, String> csrf(@RequestAttribute("_csrf") CsrfToken token) {
		return Map.of("token", token.getToken());
	}

	@GetMapping("/session")
	Map<String, String> session(Principal principal) {
		return Map.of("email", principal.getName());
	}
}
