package com.exptrack.expense.dto;

import java.util.List;

public record DashboardResponse(String month, List<DashboardCurrencyResponse> currencies, List<ExpenseResponse> recentExpenses) {
}
