package com.exptrack.user.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountServiceTest {

	@Test
	void concurrentSessionInvalidationDoesNotFailAfterPasswordCommit() {
		HttpSession session = mock(HttpSession.class);
		doThrow(new IllegalStateException()).when(session).invalidate();

		assertThatCode(() -> AccountService.invalidateCurrentSession(session)).doesNotThrowAnyException();
		verify(session).invalidate();
	}
}
