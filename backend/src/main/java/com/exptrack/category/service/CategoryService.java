package com.exptrack.category.service;

import java.util.List;

import com.exptrack.category.dto.CategoryResponse;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

	private static final List<CategoryResponse> CATEGORIES = List.of(
			new CategoryResponse("Dining"),
			new CategoryResponse("Education"),
			new CategoryResponse("Entertainment"),
			new CategoryResponse("Fuel"),
			new CategoryResponse("Gifts & Donations"),
			new CategoryResponse("Groceries"),
			new CategoryResponse("Healthcare"),
			new CategoryResponse("Housing"),
			new CategoryResponse("Insurance"),
			new CategoryResponse("Other"),
			new CategoryResponse("Personal Care"),
			new CategoryResponse("Shopping"),
			new CategoryResponse("Subscriptions"),
			new CategoryResponse("Transportation"),
			new CategoryResponse("Travel"),
			new CategoryResponse("Utilities"));

	public List<CategoryResponse> list() {
		return CATEGORIES;
	}
}
