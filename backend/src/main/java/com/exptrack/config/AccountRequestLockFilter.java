package com.exptrack.config;

import java.io.IOException;
import java.security.Principal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class AccountRequestLockFilter extends OncePerRequestFilter {

	static final String LOGIN_PATH = "/api/auth/login";
	static final String PASSWORD_PATH = "/api/account/password";

	private final AccountRequestLockCoordinator coordinator;

	AccountRequestLockFilter(AccountRequestLockCoordinator coordinator) {
		this.coordinator = coordinator;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equals(request.getMethod()) || !isAccountRequest(request);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		coordinator.execute(accountKey(request), () -> chain.doFilter(request, response));
	}

	private boolean isAccountRequest(HttpServletRequest request) {
		String path = request.getServletPath();
		return LOGIN_PATH.equals(path) || PASSWORD_PATH.equals(path);
	}

	private String accountKey(HttpServletRequest request) {
		if (LOGIN_PATH.equals(request.getServletPath())) {
			return request.getParameter("username");
		}
		Principal principal = request.getUserPrincipal();
		if (principal != null) {
			return principal.getName();
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null || authentication instanceof AnonymousAuthenticationToken
				? null : authentication.getName();
	}
}
