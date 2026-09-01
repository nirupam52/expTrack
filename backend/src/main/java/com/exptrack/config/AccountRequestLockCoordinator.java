package com.exptrack.config;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.servlet.ServletException;

import org.springframework.stereotype.Component;

@Component
final class AccountRequestLockCoordinator {

	private final ConcurrentHashMap<String, AccountLock> locks = new ConcurrentHashMap<>();

	void execute(String accountKey, RequestAction action) throws IOException, ServletException {
		String normalized = normalize(accountKey);
		if (normalized == null) {
			action.run();
			return;
		}
		AccountLock accountLock = retain(normalized);
		try {
			accountLock.lock.lock();
			action.run();
		}
		finally {
			accountLock.lock.unlock();
			release(normalized, accountLock);
		}
	}

	private AccountLock retain(String accountKey) {
		return locks.compute(accountKey, (ignored, current) -> {
			AccountLock accountLock = current == null ? new AccountLock() : current;
			accountLock.references++;
			return accountLock;
		});
	}

	private void release(String accountKey, AccountLock accountLock) {
		locks.computeIfPresent(accountKey, (ignored, current) -> {
			if (current != accountLock) return current;
			current.references--;
			return current.references == 0 ? null : current;
		});
	}

	private String normalize(String accountKey) {
		if (accountKey == null) {
			return null;
		}
		String normalized = accountKey.strip().toLowerCase(Locale.ROOT);
		return normalized.isEmpty() ? null : normalized;
	}

	private static final class AccountLock {
		private final ReentrantLock lock = new ReentrantLock();
		private int references;
	}

	@FunctionalInterface
	interface RequestAction {
		void run() throws IOException, ServletException;
	}
}
