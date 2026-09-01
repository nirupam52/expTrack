package com.exptrack.config;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AccountRequestLockTest {

	@Test
	void equivalentCaseAndWhitespaceKeysAreSerializedThroughTheFilter() throws Exception {
		AccountRequestLockFilter filter = new AccountRequestLockFilter(new AccountRequestLockCoordinator());
		ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2, Thread.ofVirtual().factory());
		ContentionLatches latches = new ContentionLatches();
		try {
			Future<?> first = submitFirst(executor, filter, latches);
			await(latches.firstEntered);
			Future<?> second = submitSecond(executor, filter, latches);
			await(latches.secondKeyRead);
			latches.allowSecond.countDown();
			assertBlocked(second);
			latches.releaseFirst.countDown();
			first.get(1, TimeUnit.SECONDS); second.get(1, TimeUnit.SECONDS);
			assertThat(latches.secondEntered.getCount()).isZero();
		}
		finally {
			latches.releaseFirst.countDown();
			executor.shutdownNow();
		}
	}
	private static Future<?> submitFirst(ExecutorService executor, AccountRequestLockFilter filter,
			ContentionLatches latches) {
		return executor.submit(() -> runFilter(filter, loginRequest(),
				blockingChain(latches.firstEntered, latches.releaseFirst)));
	}

	private static Future<?> submitSecond(ExecutorService executor, AccountRequestLockFilter filter,
			ContentionLatches latches) {
		return executor.submit(() -> runFilter(filter,
				passwordRequest(latches.secondKeyRead, latches.allowSecond),
				signallingChain(latches.secondEntered)));
	}

	@Test
	void lockIsReleasedWhenActionThrows() throws Exception {
		AccountRequestLockCoordinator coordinator = new AccountRequestLockCoordinator();
		IOException failure = new IOException("request failed");
		assertThatThrownBy(() -> coordinator.execute(" account@example.com ", () -> {
			throw failure;
		})).isSameAs(failure);

		ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
		AtomicBoolean executed = new AtomicBoolean();
		try {
			Future<?> followUp = executor.submit(() -> run(coordinator, "ACCOUNT@EXAMPLE.COM", () -> executed.set(true)));
			followUp.get(1, TimeUnit.SECONDS);
			assertThat(executed.get()).isTrue();
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	void postLoginUsesUsernameAsAccountKey() throws Exception {
		AccountRequestLockCoordinator coordinator = coordinatorThatRunsActions();
		AccountRequestLockFilter filter = new AccountRequestLockFilter(coordinator);
		MockHttpServletRequest request = request("POST", AccountRequestLockFilter.LOGIN_PATH);
		request.setParameter("username", "Alice@example.com");
		AtomicInteger chainInvocations = new AtomicInteger();

		filter.doFilter(request, new MockHttpServletResponse(), chain(chainInvocations));

		verify(coordinator).execute(eq("Alice@example.com"), any());
		assertThat(chainInvocations.get()).isEqualTo(1);
	}

	@Test
	void postPasswordUsesPrincipalAsAccountKey() throws Exception {
		AccountRequestLockCoordinator coordinator = coordinatorThatRunsActions();
		AccountRequestLockFilter filter = new AccountRequestLockFilter(coordinator);
		MockHttpServletRequest request = request("POST", AccountRequestLockFilter.PASSWORD_PATH);
		request.setUserPrincipal(() -> "Alice@example.com");
		AtomicInteger chainInvocations = new AtomicInteger();

		filter.doFilter(request, new MockHttpServletResponse(), chain(chainInvocations));

		verify(coordinator).execute(eq("Alice@example.com"), any());
		assertThat(chainInvocations.get()).isEqualTo(1);
	}

	@Test
	void putDefaultCurrencyUsesPrincipalAsAccountKey() throws Exception {
		AccountRequestLockCoordinator coordinator = coordinatorThatRunsActions();
		AccountRequestLockFilter filter = new AccountRequestLockFilter(coordinator);
		MockHttpServletRequest request = request("PUT", AccountRequestLockFilter.DEFAULT_CURRENCY_PATH);
		request.setUserPrincipal(() -> "Alice@example.com");
		AtomicInteger chainInvocations = new AtomicInteger();

		filter.doFilter(request, new MockHttpServletResponse(), chain(chainInvocations));

		verify(coordinator).execute(eq("Alice@example.com"), any());
		assertThat(chainInvocations.get()).isEqualTo(1);
	}

	@ParameterizedTest
	@CsvSource({"GET, /api/auth/login", "POST, /api/account/other"})
	void nonTargetRequestsBypassCoordinator(String method, String path) throws Exception {
		AccountRequestLockCoordinator coordinator = coordinatorThatRunsActions();
		AccountRequestLockFilter filter = new AccountRequestLockFilter(coordinator);
		AtomicInteger chainInvocations = new AtomicInteger();

		filter.doFilter(request(method, path), new MockHttpServletResponse(), chain(chainInvocations));

		verifyNoInteractions(coordinator);
		assertThat(chainInvocations.get()).isEqualTo(1);
	}

	@ParameterizedTest
	@CsvSource({"POST, /api/auth/login", "POST, /api/account/password", "PUT, /api/account/default-currency"})
	void missingAccountKeysPassThrough(String method, String path) throws Exception {
		SecurityContextHolder.clearContext();
		AccountRequestLockCoordinator coordinator = coordinatorThatRunsActions();
		AccountRequestLockFilter filter = new AccountRequestLockFilter(coordinator);
		AtomicInteger chainInvocations = new AtomicInteger();

		filter.doFilter(request(method, path), new MockHttpServletResponse(), chain(chainInvocations));

		verify(coordinator).execute(isNull(), any());
		assertThat(chainInvocations.get()).isEqualTo(1);
	}

	private static AccountRequestLockCoordinator coordinatorThatRunsActions() throws Exception {
		AccountRequestLockCoordinator coordinator = mock(AccountRequestLockCoordinator.class);
		doAnswer(invocation -> {
			AccountRequestLockCoordinator.RequestAction action = invocation.getArgument(1);
			action.run();
			return null;
		}).when(coordinator).execute(any(), any());
		return coordinator;
	}

	private static MockHttpServletRequest request(String method, String path) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setServletPath(path);
		return request;
	}
	private static MockHttpServletRequest loginRequest() {
		MockHttpServletRequest request = request("POST", AccountRequestLockFilter.LOGIN_PATH);
		request.setParameter("username", " Alice@example.com ");
		return request;
	}

	private static MockHttpServletRequest passwordRequest(CountDownLatch keyRead, CountDownLatch allowLookup) {
		MockHttpServletRequest request = request("POST", AccountRequestLockFilter.PASSWORD_PATH);
		request.setUserPrincipal(() -> {
			keyRead.countDown();
			await(allowLookup);
			return "alice@EXAMPLE.com";
		});
		return request;
	}

	private static FilterChain blockingChain(CountDownLatch entered, CountDownLatch release) {
		return (request, response) -> {
			entered.countDown();
			await(release);
		};
	}

	private static FilterChain signallingChain(CountDownLatch entered) {
		return (request, response) -> entered.countDown();
	}

	private static void runFilter(AccountRequestLockFilter filter, MockHttpServletRequest request,
			FilterChain chain) {
		try {
			filter.doFilter(request, new MockHttpServletResponse(), chain);
		}
		catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}
	private static void assertBlocked(Future<?> request) throws Exception {
		assertThatThrownBy(() -> request.get(100, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
	}

	private static FilterChain chain(AtomicInteger invocations) {
		return (request, response) -> invocations.incrementAndGet();
	}

	private static void run(AccountRequestLockCoordinator coordinator, String key,
			AccountRequestLockCoordinator.RequestAction action) {
		try {
			coordinator.execute(key, action);
		}
		catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(1, TimeUnit.SECONDS)) {
				throw new AssertionError("timed out waiting for first request");
			}
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
	}
	private static final class ContentionLatches {
		private final CountDownLatch firstEntered = new CountDownLatch(1);
		private final CountDownLatch releaseFirst = new CountDownLatch(1);
		private final CountDownLatch secondKeyRead = new CountDownLatch(1);
		private final CountDownLatch allowSecond = new CountDownLatch(1);
		private final CountDownLatch secondEntered = new CountDownLatch(1);
	}
}
