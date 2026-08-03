package com.exptrack.category.service;

import java.util.List;

import com.exptrack.category.dto.CategoryResponse;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

	private static final List<String> NAMES = List.of(
			"Dining",
			"Education",
			"Entertainment",
			"Fuel",
			"Gifts & Donations",
			"Groceries",
			"Healthcare",
			"Housing",
			"Insurance",
			"Other",
			"Personal Care",
			"Shopping",
			"Subscriptions",
			"Transportation",
			"Travel",
			"Utilities");

	public List<CategoryResponse> list() {
		return NAMES.stream()
				.map(CategoryResponse::new)
				.toList();
	}
}
