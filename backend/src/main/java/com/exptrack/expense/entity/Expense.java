package com.exptrack.expense.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(nullable = false)
	private String title;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Column(name = "category_id", nullable = false)
	private int categoryId;

	@Column(name = "expense_date", nullable = false)
	private LocalDate expenseDate;

	@Column(nullable = false)
	private String currency;

	private String note;

	protected Expense() {
	}

	public Expense(Integer userId, ExpenseDetails details) {
		this.userId = userId;
		this.title = details.title();
		this.amountMinor = details.amountMinor();
		this.categoryId = details.categoryId();
		this.expenseDate = details.expenseDate();
		this.currency = details.currency();
		this.note = details.note();
	}

	public void update(ExpenseDetails details) {
		this.title = details.title();
		this.amountMinor = details.amountMinor();
		this.categoryId = details.categoryId();
		this.expenseDate = details.expenseDate();
		this.note = details.note();
	}

	public Integer getId() {
		return id;
	}

	public Integer getUserId() {
		return userId;
	}

	public String getTitle() {
		return title;
	}

	public long getAmountMinor() {
		return amountMinor;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public LocalDate getExpenseDate() {
		return expenseDate;
	}

	public String getCurrency() {
		return currency;
	}

	public String getNote() {
		return note;
	}
}
