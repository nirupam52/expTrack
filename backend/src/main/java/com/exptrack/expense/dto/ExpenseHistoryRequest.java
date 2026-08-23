package com.exptrack.expense.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ExpenseHistoryRequest(
		@Size(max = 500) String query,
		Integer categoryId,
		LocalDate from,
		LocalDate to,
		@Size(max = 64) String cursor,
		@Min(1) @Max(50) Integer limit) {
}
