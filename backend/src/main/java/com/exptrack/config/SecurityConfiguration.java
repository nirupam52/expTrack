package com.exptrack.config;

import com.exptrack.user.repository.UserAccountRepository;
import com.exptrack.user.service.PreHashingBCryptEncoder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthRateLimitFilter authRateLimitFilter,
			SessionRegistry sessionRegistry) throws Exception {
		RequestMatcher apiRequests = request -> request.getRequestURI().startsWith("/api/");
		http.addFilterBefore(authRateLimitFilter, CsrfFilter.class);
		configureAuthorization(http);
		configureCsrf(http);
		configureExceptionHandling(http, apiRequests);
		configureSessionManagement(http, sessionRegistry);
		configureLogin(http);
		configureLogout(http);
		return http.build();
	}

	private void configureAuthorization(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/index.html", "/robots.txt", "/_app/**", "/actuator/health",
						"/api/auth/register", "/api/auth/csrf", "/error").permitAll()
				.anyRequest().authenticated());
	}

	private void configureCsrf(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
	}

	private void configureExceptionHandling(HttpSecurity http, RequestMatcher apiRequests) throws Exception {
		http.exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
				new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiRequests));
	}

	private void configureSessionManagement(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
		http.sessionManagement(session -> session.maximumSessions(Integer.MAX_VALUE)
				.sessionRegistry(sessionRegistry)
				.expiredSessionStrategy(event -> event.getResponse().sendError(HttpStatus.UNAUTHORIZED.value())));
	}

	private void configureLogin(HttpSecurity http) throws Exception {
		http.formLogin(form -> form.loginProcessingUrl("/api/auth/login")
				.successHandler((request, response, authentication) ->
						response.setStatus(HttpStatus.NO_CONTENT.value()))
				.failureHandler((request, response, exception) ->
						response.sendError(HttpStatus.UNAUTHORIZED.value()))
				.permitAll());
	}

	private void configureLogout(HttpSecurity http) throws Exception {
		http.logout(logout -> logout.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler((request, response, authentication) ->
						response.setStatus(HttpStatus.NO_CONTENT.value())));
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
