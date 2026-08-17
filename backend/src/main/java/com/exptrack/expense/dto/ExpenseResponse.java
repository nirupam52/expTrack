package com.exptrack.expense.dto;

import java.time.LocalDate;

public record ExpenseResponse(Integer id, String title, long amountMinor, String category, LocalDate date, String currency, String note) {
}
