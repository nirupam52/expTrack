package com.exptrack.config;

import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthRateLimitFilter authRateLimitFilter) throws Exception {
		RequestMatcher apiRequests = request -> request.getRequestURI().startsWith("/api/");
		return http.addFilterBefore(authRateLimitFilter, CsrfFilter.class)
				.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/index.html", "/robots.txt", "/_app/**", "/actuator/health", "/api/auth/register", "/api/auth/csrf", "/error").permitAll()
				.anyRequest().authenticated())
				.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiRequests))
				.formLogin(form -> form.loginProcessingUrl("/api/auth/login")
						.successHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value()))
						.failureHandler((request, response, exception) -> response.sendError(HttpStatus.UNAUTHORIZED.value()))
						.permitAll())
		.logout(logout -> logout.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value())))
				.build();
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
		return new BCryptPasswordEncoder();
	}
}
