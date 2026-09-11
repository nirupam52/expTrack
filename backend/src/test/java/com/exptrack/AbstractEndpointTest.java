package com.exptrack;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

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
 */
public abstract class AbstractEndpointTest {

	@Autowired
	protected JdbcTemplate jdbc;

	@BeforeEach
	void resetDatabase() {
		jdbc.update("DELETE FROM expenses");
		jdbc.update("DELETE FROM users");
	}
}
