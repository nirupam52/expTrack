package com.exptrack.expense.repository;

import java.util.List;

import com.exptrack.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

	List<Expense> findTop10ByUserIdOrderByExpenseDateDescIdDesc(Integer userId);
}
