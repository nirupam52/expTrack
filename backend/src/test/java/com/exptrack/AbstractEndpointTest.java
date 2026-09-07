package com.exptrack;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for {@code @SpringBootTest} endpoint tests.
 *
 * <p>Spring caches the {@code ApplicationContext} (and therefore the single in-memory
 * database connection) across every test class that declares the same
 * {@code @SpringBootTest(properties = ...)}. Without an explicit reset, rows created by
 * one test method or test class remain visible to the next, which lets unrelated tests
 * collide on shared fixture data (for example two "Coffee" titled expenses owned by
 * different users). Clearing the tables before every test guarantees each test starts
 * from an empty, owner-scoped database regardless of context sharing or execution order.
 *
 * <p>A single PostgreSQL container is started once for the whole test JVM (via the
 * static initializer below) and shared by every subclass through
 * {@link DynamicPropertySource}, so every endpoint test class runs against the same
 * PostgreSQL instance instead of spinning up one container per class.
 */
public abstract class AbstractEndpointTest {

	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	protected JdbcTemplate jdbc;

	@BeforeEach
	void resetDatabase() {
		jdbc.update("TRUNCATE TABLE expenses, users RESTART IDENTITY CASCADE");
	}
}
