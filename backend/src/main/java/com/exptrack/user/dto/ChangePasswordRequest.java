package com.exptrack.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
		@NotBlank String currentPassword,
		String newPassword,
		String newPasswordConfirmation) {
}
