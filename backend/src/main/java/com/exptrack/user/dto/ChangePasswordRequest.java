package com.exptrack.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotNull @Size(max = 128) String currentPassword,
		@NotNull @Size(max = 128) String newPassword,
		@NotNull @Size(max = 128) String newPasswordConfirmation) {
}
