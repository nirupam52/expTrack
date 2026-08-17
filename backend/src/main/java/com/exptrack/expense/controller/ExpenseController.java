package com.exptrack.expense.controller;

import java.security.Principal;
import java.util.List;

import com.exptrack.expense.dto.ExpenseRequest;
import com.exptrack.expense.dto.ExpenseResponse;
import com.exptrack.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

	private final ExpenseService expenses;

	public ExpenseController(ExpenseService expenses) {
		this.expenses = expenses;
	}

	@GetMapping
	List<ExpenseResponse> recent(Principal principal) {
		return expenses.recent(principal.getName());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ExpenseResponse create(@Valid @RequestBody ExpenseRequest request, Principal principal) {
		return expenses.create(request, principal.getName());
	}
}
