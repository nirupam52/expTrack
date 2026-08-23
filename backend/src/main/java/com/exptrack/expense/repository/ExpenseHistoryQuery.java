package com.exptrack.expense.repository;

import java.time.LocalDate;

public record ExpenseHistoryQuery(
		Integer userId,
		String text,
		Integer categoryId,
		LocalDate fromDate,
		LocalDate toDate,
		LocalDate cursorDate,
		Integer cursorId,
		int limit) {
}
