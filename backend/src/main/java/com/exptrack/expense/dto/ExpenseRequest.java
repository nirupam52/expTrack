package com.exptrack.expense.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExpenseRequest(
		@NotBlank @Size(max = 120) String title,
		@NotBlank @Size(max = 30) String amount,
		@NotNull Integer categoryId,
		@NotNull LocalDate date,
		@Size(max = 500) String note,
		@Size(max = 3) String currency) {
}
