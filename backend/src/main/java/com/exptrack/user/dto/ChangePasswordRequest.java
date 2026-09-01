package com.exptrack.user.dto;

import jakarta.validation.constraints.NotNull;

public record ChangePasswordRequest(
		@NotNull String currentPassword,
		@NotNull String newPassword,
		@NotNull String newPasswordConfirmation) {
}
