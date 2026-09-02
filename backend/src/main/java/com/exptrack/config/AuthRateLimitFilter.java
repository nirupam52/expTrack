package com.exptrack.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final int MAX_TRACKED_CLIENTS = 10_000;
	// Process-local limiter with a bounded client table; use shared infrastructure for multiple backend instances.
	private final Map<String, Attempts> attempts = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Attempts> eldest) {
			return size() > MAX_TRACKED_CLIENTS;
		}
	};
	private final int maxAttempts;
	private final long windowMillis;

	AuthRateLimitFilter(@Value("${exptrack.auth.max-attempts:10}") int maxAttempts,
			@Value("${exptrack.auth.window-seconds:60}") long windowSeconds) {
		this.maxAttempts = Math.max(1, maxAttempts);
		this.windowMillis = Math.max(1, windowSeconds) * 1_000;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equals(request.getMethod()) || !("/api/auth/register".equals(request.getServletPath())
				|| "/api/auth/login".equals(request.getServletPath())
				|| "/api/account/password".equals(request.getServletPath()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (!accepts(clientKey(request)) || !acceptsAccount(request)) {
			response.setStatus(429);
			return;
		}
		chain.doFilter(request, response);
	}

	private String clientKey(HttpServletRequest request) {
		String key = request.getRemoteAddr() + "|" + request.getServletPath();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken ? key : key + "|" + authentication.getName();
	}

	private boolean acceptsAccount(HttpServletRequest request) {
		String account = accountKey(request);
		return account == null || accepts("account|" + request.getServletPath() + "|" + account);
	}

	private String accountKey(HttpServletRequest request) {
		String account = "/api/auth/login".equals(request.getServletPath())
				? request.getParameter("username") : authenticatedAccount();
		if (account == null || account.isBlank()) return null;
		return account.strip().toLowerCase(Locale.ROOT);
	}

	private String authenticatedAccount() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken ? null : authentication.getName();
	}

	private synchronized boolean accepts(String client) {
		long now = System.currentTimeMillis();
		Attempts current = attempts.get(client);
		if (current == null || now - current.windowStartedAt() >= windowMillis) {
			attempts.put(client, new Attempts(now, 1));
			return true;
		}
		attempts.put(client, new Attempts(current.windowStartedAt(), current.count() + 1));
		return current.count() < maxAttempts;
	}

	private record Attempts(long windowStartedAt, int count) {
	}
}
