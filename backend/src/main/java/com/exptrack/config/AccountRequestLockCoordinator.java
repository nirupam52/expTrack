package com.exptrack.config;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.servlet.ServletException;

import org.springframework.stereotype.Component;

@Component
final class AccountRequestLockCoordinator {

	private static final int STRIPE_COUNT = 64;
	private final ReentrantLock[] stripes = createStripes();

	void execute(String accountKey, RequestAction action) throws IOException, ServletException {
		ReentrantLock lock = lockFor(accountKey);
		if (lock == null) {
			action.run();
			return;
		}
		lock.lock();
		try {
			action.run();
		} finally {
			lock.unlock();
		}
	}

	private ReentrantLock lockFor(String accountKey) {
		String normalized = normalize(accountKey);
		return normalized == null ? null : stripes[Math.floorMod(normalized.hashCode(), STRIPE_COUNT)];
	}

	private String normalize(String accountKey) {
		if (accountKey == null) {
			return null;
		}
		String normalized = accountKey.strip().toLowerCase(Locale.ROOT);
		return normalized.isEmpty() ? null : normalized;
	}

	private ReentrantLock[] createStripes() {
		ReentrantLock[] locks = new ReentrantLock[STRIPE_COUNT];
		for (int index = 0; index < locks.length; index++) {
			locks[index] = new ReentrantLock();
		}
		return locks;
	}

	@FunctionalInterface
	interface RequestAction {
		void run() throws IOException, ServletException;
	}
}
