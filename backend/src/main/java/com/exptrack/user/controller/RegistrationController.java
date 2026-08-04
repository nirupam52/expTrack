package com.exptrack.user.controller;

import com.exptrack.user.dto.RegistrationRequest;
import com.exptrack.user.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

	private final RegistrationService registrationService;

	public RegistrationController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	void register(@Valid @RequestBody RegistrationRequest request) {
		registrationService.register(request);
	}
}
