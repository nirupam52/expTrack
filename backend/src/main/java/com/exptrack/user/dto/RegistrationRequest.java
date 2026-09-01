package com.exptrack.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
		@NotBlank @Email @Size(max = 254) String email,
		String password,
		@NotBlank String defaultCurrency) {
}
