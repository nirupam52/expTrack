package com.exptrack.expense.controller;

import java.security.Principal;

import com.exptrack.expense.dto.ExpenseHistoryRequest;
import com.exptrack.expense.dto.ExpensePageResponse;
import com.exptrack.expense.dto.ExpenseRequest;
import com.exptrack.expense.dto.ExpenseResponse;
import com.exptrack.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
	ExpensePageResponse history(@Valid @ModelAttribute ExpenseHistoryRequest request, Principal principal) {
		return expenses.history(request, principal.getName());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ExpenseResponse create(@Valid @RequestBody ExpenseRequest request, Principal principal) {
		return expenses.create(request, principal.getName());
	}

	@PutMapping("/{expenseId}")
	ExpenseResponse update(@PathVariable Integer expenseId, @Valid @RequestBody ExpenseRequest request, Principal principal) {
		return expenses.update(expenseId, request, principal.getName());
	}

	@DeleteMapping("/{expenseId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Integer expenseId, Principal principal) {
		expenses.delete(expenseId, principal.getName());
	}
}
