package com.exptrack.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DefaultCurrencyRequest(@NotBlank String defaultCurrency) {
}
