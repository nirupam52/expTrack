package com.exptrack.expense.entity;

import java.time.LocalDate;

public record ExpenseDetails(
		String title,
		long amountMinor,
		int categoryId,
		LocalDate expenseDate,
		String currency,
		String note) {
}
