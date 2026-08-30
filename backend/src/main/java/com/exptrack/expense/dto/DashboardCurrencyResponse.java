package com.exptrack.expense.dto;

import java.util.List;

public record DashboardCurrencyResponse(String currency, String totalMinor, List<DashboardCategoryResponse> categories) {
}
