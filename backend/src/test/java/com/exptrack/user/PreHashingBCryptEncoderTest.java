package com.exptrack.user;

import java.nio.charset.StandardCharsets;

import com.exptrack.user.service.PreHashingBCryptEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PreHashingBCryptEncoderTest {

	private final PreHashingBCryptEncoder encoder = new PreHashingBCryptEncoder();
	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	@Test
	void retainsBCryptCompatibilityAtThe72Utf8ByteBoundary() {
		String rawPassword = "a".repeat(70) + "é";

		assertThat(rawPassword.getBytes(StandardCharsets.UTF_8)).hasSize(72);
		assertThat(encoder.matches(rawPassword, bcrypt.encode(rawPassword))).isTrue();
	}

	@Test
	void retainsLegacyBCryptCompatibilityAbove72Utf8Bytes() {
		String rawPassword = "é".repeat(40);
		String encoded = bcrypt.encode(rawPassword);

		assertThat(rawPassword.getBytes(StandardCharsets.UTF_8)).hasSize(80);
		assertThat(encoder.matches(rawPassword, encoded)).isTrue();
	}

	@Test
	void preHashesPasswordsBeyondThe72Utf8ByteBoundary() {
		String boundaryPassword = "a".repeat(70) + "é";
		String longerPassword = boundaryPassword + "a";
		String encoded = encoder.encode(longerPassword);

		assertThat(longerPassword.getBytes(StandardCharsets.UTF_8)).hasSize(73);
		assertThat(encoder.matches(longerPassword, encoded)).isTrue();
		assertThat(encoder.matches(boundaryPassword, encoded)).isFalse();
	}

	@Test
	void preventsBCryptTruncationCollisionsFor73ByteAsciiPasswords() {
		String boundaryPassword = "a".repeat(72);
		String longerPassword = "a".repeat(73);

		assertThat(encoder.matches(longerPassword, encoder.encode(boundaryPassword))).isFalse();
		assertThat(encoder.matches(boundaryPassword, encoder.encode(longerPassword))).isFalse();
	}
}
