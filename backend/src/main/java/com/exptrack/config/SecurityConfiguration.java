package com.exptrack.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.exptrack.user.repository.UserAccountRepository;
import com.exptrack.user.service.PreHashingBCryptEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthRateLimitFilter authRateLimitFilter,
			SessionRegistry sessionRegistry) throws Exception {
		RequestMatcher apiRequests = request -> request.getRequestURI().startsWith("/api/");
		return http.addFilterBefore(authRateLimitFilter, CsrfFilter.class)
				.addFilterAt(new ConcurrentSessionFilter(sessionRegistry,
						event -> event.getResponse().sendError(HttpStatus.UNAUTHORIZED.value())),
						ConcurrentSessionFilter.class)
				.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/index.html", "/robots.txt", "/_app/**", "/actuator/health", "/api/auth/register", "/api/auth/csrf", "/error").permitAll()
				.anyRequest().authenticated())
				.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiRequests))
				.formLogin(form -> form.loginProcessingUrl("/api/auth/login")
						.successHandler((request, response, authentication) -> {
							String sessionId = request.getSession().getId();
							sessionRegistry.removeSessionInformation(sessionId);
							sessionRegistry.registerNewSession(sessionId, authentication.getName());
							response.setStatus(HttpStatus.NO_CONTENT.value());
						})
						.failureHandler((request, response, exception) -> response.sendError(HttpStatus.UNAUTHORIZED.value()))
						.permitAll())
		.logout(logout -> logout.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value())))
				.build();
	}

	@Bean
	SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	UserDetailsService userDetailsService(UserAccountRepository users) {
		return email -> users.findByEmailIgnoreCase(email)
				.map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
						.password(user.getPasswordHash())
						.authorities("USER")
						.build())
				.orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(email));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new PreHashingBCryptEncoder();
	}
}
