package com.exptrack.category.service;

import java.util.List;
import java.util.Map;

import com.exptrack.category.dto.CategoryResponse;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

	private static final Map<Integer, String> CATEGORIES = Map.ofEntries(
			Map.entry(1, "Dining"),
			Map.entry(2, "Education"),
			Map.entry(3, "Entertainment"),
			Map.entry(4, "Fuel"),
			Map.entry(5, "Gifts & Donations"),
			Map.entry(6, "Groceries"),
			Map.entry(7, "Healthcare"),
			Map.entry(8, "Housing"),
			Map.entry(9, "Insurance"),
			Map.entry(10, "Other"),
			Map.entry(11, "Personal Care"),
			Map.entry(12, "Shopping"),
			Map.entry(13, "Subscriptions"),
			Map.entry(14, "Transportation"),
			Map.entry(15, "Travel"),
			Map.entry(16, "Utilities"));

	private static final List<CategoryResponse> RESPONSES = CATEGORIES.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> new CategoryResponse(entry.getKey(), entry.getValue()))
			.toList();

	public List<CategoryResponse> list() {
		return RESPONSES;
	}

	public boolean isValid(Integer id) {
		return id != null && CATEGORIES.containsKey(id);
	}
}
