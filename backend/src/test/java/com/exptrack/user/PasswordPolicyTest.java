package com.exptrack.user;

import com.exptrack.user.service.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

	@ParameterizedTest
	@CsvSource({"14, false", "15, true", "64, true", "65, false"})
	void acceptsPasswordsOnlyWithinInclusiveCodePointBoundaries(int codePoints, boolean expected) {
		assertThat(PasswordPolicy.accepts("a".repeat(codePoints))).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({"14, false", "15, true", "64, true", "65, false"})
	void measuresUnicodePasswordsInCodePoints(int codePoints, boolean expected) {
		assertThat(PasswordPolicy.accepts("\uD83D\uDE00".repeat(codePoints))).isEqualTo(expected);
	}

	@Test
	void acceptsWhitespaceOnlyPasswordsWhenTheirCodePointLengthIsValid() {
		assertThat(PasswordPolicy.accepts(" ".repeat(15))).isTrue();
	}
}
