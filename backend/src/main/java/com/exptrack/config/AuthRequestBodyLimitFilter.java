package com.exptrack.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ReadListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
final class AuthRequestBodyLimitFilter extends OncePerRequestFilter {
	static final int MAX_BODY_BYTES = 16 * 1024;

	static final String LOGIN_PATH = "/api/auth/login";
	static final String PASSWORD_PATH = "/api/account/password";
	static final String CURRENCY_PATH = "/api/account/default-currency";
	private static final int MAX_FORM_PASSWORD_CODE_POINTS = 128;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !isProtectedRequest(request);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (request.getContentLengthLong() > MAX_BODY_BYTES || oversizedFormPassword(request)) {
			response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value());
			return;
		}
		chain.doFilter(new LimitedRequest(request), response);
	}

	private boolean isProtectedRequest(HttpServletRequest request) {
		String method = request.getMethod();
		String path = request.getServletPath();
		return ("POST".equals(method) && (LOGIN_PATH.equals(path) || "/api/auth/register".equals(path)
				|| PASSWORD_PATH.equals(path))) || ("PUT".equals(method) && CURRENCY_PATH.equals(path));
	}

	private boolean oversizedFormPassword(HttpServletRequest request) throws IOException {
		if (!LOGIN_PATH.equals(request.getServletPath())) return false;
		request.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String password = request.getParameter("password");
		return password != null && password.codePointCount(0, password.length()) > MAX_FORM_PASSWORD_CODE_POINTS;
	}

	private static final class LimitedRequest extends HttpServletRequestWrapper {
		private final LimitedInputStream input;

		private LimitedRequest(HttpServletRequest request) throws IOException {
			super(request);
			this.input = new LimitedInputStream(request.getInputStream());
		}

		@Override
		public ServletInputStream getInputStream() {
			return input;
		}

		@Override
		public BufferedReader getReader() {
			return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
		}

		@Override
		public int getContentLength() {
			return Math.min(super.getContentLength(), MAX_BODY_BYTES);
		}

		@Override
		public long getContentLengthLong() {
			return Math.min(super.getContentLengthLong(), MAX_BODY_BYTES);
		}
	}

	private static final class LimitedInputStream extends ServletInputStream {
		private final ServletInputStream delegate;
		private int bytesRead;
		private boolean limitReached;

		private LimitedInputStream(ServletInputStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public int read() throws IOException {
			if (bytesRead >= MAX_BODY_BYTES) {
				limitReached = true;
				return -1;
			}
			int value = delegate.read();
			if (value >= 0) bytesRead++;
			return value;
		}

		@Override
		public int read(byte[] bytes, int offset, int length) throws IOException {
			if (bytesRead >= MAX_BODY_BYTES) {
				limitReached = true;
				return -1;
			}
			int allowed = Math.min(length, MAX_BODY_BYTES - bytesRead);
			int read = delegate.read(bytes, offset, allowed);
			if (read > 0) bytesRead += read;
			return read;
		}

		@Override
		public boolean isFinished() {
			return limitReached || delegate.isFinished();
		}

		@Override
		public boolean isReady() {
			return delegate.isReady();
		}

		@Override
		public void setReadListener(ReadListener listener) {
			delegate.setReadListener(listener);
		}
	}
}
