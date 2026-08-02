package com.exptrack.config;

import com.exptrack.user.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthRateLimitFilter authRateLimitFilter) throws Exception {
		return http.addFilterBefore(authRateLimitFilter, CsrfFilter.class)
				.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health", "/api/auth/register", "/api/auth/csrf").permitAll()
				.anyRequest().authenticated())
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
						.ignoringRequestMatchers("/api/auth/register"))
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
