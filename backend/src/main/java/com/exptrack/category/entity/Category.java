package com.exptrack.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(nullable = false)
	private String name;

	protected Category() {
	}

	public Category(Integer userId, String name) {
		this.userId = userId;
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
