package com.exptrack.user.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "default_currency", nullable = false)
	private String defaultCurrency;

	@Column(name = "created_at")
	private String createdAt;

	protected UserAccount() {
	}

	public UserAccount(String email, String passwordHash, String defaultCurrency) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.defaultCurrency = defaultCurrency;
		this.createdAt = Instant.now().toString();
	}

	public String getEmail() {
		return email;
	}

	public Integer getId() {
		return id;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDefaultCurrency() {
		return defaultCurrency;
	}

	public Instant getCreatedAt() {
		return createdAt == null ? null : Instant.parse(createdAt);
	}

	public void setDefaultCurrency(String defaultCurrency) {
		this.defaultCurrency = defaultCurrency;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
}
