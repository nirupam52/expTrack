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

	private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

	@Override
	public String encode(CharSequence rawPassword) {
		return delegate.encode(preHash(rawPassword));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return delegate.matches(preHash(rawPassword), encodedPassword);
	}

	private String preHash(CharSequence rawPassword) {
		byte[] bytes = rawPassword.toString().getBytes(StandardCharsets.UTF_8);
		if (bytes.length <= BCRYPT_MAX_BYTES) {
			return rawPassword.toString();
		}
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
