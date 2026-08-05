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

	@Column(nullable = false)
	private String category;

	@Column(name = "expense_date", nullable = false)
	private LocalDate expenseDate;

	@Column(nullable = false)
	private String currency;

	private String note;

	protected Expense() {
	}

	public Expense(Integer userId, String title, long amountMinor, String category, LocalDate expenseDate, String currency, String note) {
		this.userId = userId;
		this.title = title;
		this.amountMinor = amountMinor;
		this.category = category;
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

	public String getCategory() {
		return category;
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
