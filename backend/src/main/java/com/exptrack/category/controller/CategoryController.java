package com.exptrack.category.controller;

import java.util.List;

import com.exptrack.category.dto.CategoryResponse;
import com.exptrack.category.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

	private final CategoryService categories;

	public CategoryController(CategoryService categories) {
		this.categories = categories;
	}

	@GetMapping
	List<CategoryResponse> list() {
		return categories.list();
	}
}
