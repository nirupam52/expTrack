package com.exptrack.expense;

import com.exptrack.expense.service.CurrencySnapshotService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencySnapshotServiceTest {

	@Test
	void snapshotSupportsEmailsContainingPayloadDelimiters() {
		String snapshot = CurrencySnapshotService.issue("a|b@example.com", "USD");

		assertThat(CurrencySnapshotService.resolve(snapshot, "a|b@example.com")).contains("USD");
	}
}
