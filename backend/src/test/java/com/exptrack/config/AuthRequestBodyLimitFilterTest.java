package com.exptrack.config;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestBodyLimitFilterTest {

	@Test
	void rejectsAuthenticationBodiesAboveTheConfiguredLimit() throws Exception {
		MockHttpServletRequest request = request("/api/auth/register");
		request.setContent(new byte[AuthRequestBodyLimitFilter.MAX_BODY_BYTES + 1]);
		MockHttpServletResponse response = new MockHttpServletResponse();

		new AuthRequestBodyLimitFilter().doFilter(request, response, unusedChain());

		assertThat(response.getStatus()).isEqualTo(413);
	}

	@Test
	void rejectsOversizedFormLoginPasswordsBeforeAuthentication() throws Exception {
		MockHttpServletRequest request = request(AuthRequestBodyLimitFilter.LOGIN_PATH);
		request.setParameter("password", "a".repeat(129));
		MockHttpServletResponse response = new MockHttpServletResponse();

		new AuthRequestBodyLimitFilter().doFilter(request, response, unusedChain());

		assertThat(response.getStatus()).isEqualTo(413);
	}

	@Test
	void rejectsOversizedCurrencyUpdatesBeforeParsing() throws Exception {
		MockHttpServletRequest request = request("PUT", AuthRequestBodyLimitFilter.CURRENCY_PATH);
		request.setContent(new byte[AuthRequestBodyLimitFilter.MAX_BODY_BYTES + 1]);
		MockHttpServletResponse response = new MockHttpServletResponse();

		new AuthRequestBodyLimitFilter().doFilter(request, response, unusedChain());

		assertThat(response.getStatus()).isEqualTo(413);
	}

	private MockHttpServletRequest request(String path) {
		return request("POST", path);
	}

	private MockHttpServletRequest request(String method, String path) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setServletPath(path);
		return request;
	}

	private FilterChain unusedChain() {
		return (request, response) -> {
			throw new AssertionError("authentication chain must not run");
		};
	}
}
