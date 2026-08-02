package com.exptrack.category.service;

import java.util.List;

import com.exptrack.category.dto.CategoryResponse;
import com.exptrack.category.repository.CategoryRepository;
import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

	private final UserAccountRepository users;
	private final CategoryRepository categories;

	public CategoryService(UserAccountRepository users, CategoryRepository categories) {
		this.users = users;
		this.categories = categories;
	}

	public List<CategoryResponse> list(String email) {
		return users.findByEmailIgnoreCase(email)
				.stream()
				.flatMap(user -> categories.findByUserIdOrderByName(user.getId()).stream())
				.map(category -> new CategoryResponse(category.getName()))
				.toList();
	}
}
