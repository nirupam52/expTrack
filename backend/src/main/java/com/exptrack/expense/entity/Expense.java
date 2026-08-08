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

	public Expense(Integer userId, String title, long amountMinor, int categoryId, LocalDate expenseDate, String currency, String note) {
		this.userId = userId;
		this.title = title;
		this.amountMinor = amountMinor;
		this.categoryId = categoryId;
		this.expenseDate = expenseDate;
		this.currency = currency;
		this.note = note;
	}

	public Integer getId() {
		return id;
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
