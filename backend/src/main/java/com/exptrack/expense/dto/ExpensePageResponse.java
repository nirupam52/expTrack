package com.exptrack.expense.dto;

import java.util.List;

public record ExpensePageResponse(List<ExpenseResponse> items, String nextCursor) {
}
