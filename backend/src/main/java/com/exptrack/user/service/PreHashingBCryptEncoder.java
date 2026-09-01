package com.exptrack.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt with SHA-256 pre-hashing for passwords longer than the 72-byte bcrypt input limit,
 * so Unicode passwords up to the 64-code-point policy limit can be stored and verified.
 */
public final class PreHashingBCryptEncoder implements PasswordEncoder {

	private static final int BCRYPT_MAX_BYTES = 72;
	private static final String BCRYPT_MARKER = "{bcrypt}";
	private static final String PRE_HASH_MARKER = "{sha256}";

	private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

	@Override
	public String encode(CharSequence rawPassword) {
		String password = rawPassword.toString();
		byte[] bytes = utf8Bytes(password);
		return bytes.length <= BCRYPT_MAX_BYTES
				? BCRYPT_MARKER + delegate.encode(password)
				: PRE_HASH_MARKER + delegate.encode(preHash(bytes));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (rawPassword == null || encodedPassword == null) return false;
		if (encodedPassword.startsWith(BCRYPT_MARKER)) {
			return utf8Bytes(rawPassword.toString()).length <= BCRYPT_MAX_BYTES
					&& delegate.matches(rawPassword, encodedPassword.substring(BCRYPT_MARKER.length()));
		}
		if (encodedPassword.startsWith(PRE_HASH_MARKER)) {
			return delegate.matches(preHash(rawPassword), encodedPassword.substring(PRE_HASH_MARKER.length()));
		}
		return utf8Bytes(rawPassword.toString()).length <= BCRYPT_MAX_BYTES
				&& delegate.matches(rawPassword, encodedPassword);
	}

	private String preHash(CharSequence rawPassword) {
		return preHash(utf8Bytes(rawPassword.toString()));
	}

	private String preHash(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private byte[] utf8Bytes(String password) {
		return password.getBytes(StandardCharsets.UTF_8);
	}
}
