package com.exptrack.expense.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class CurrencySnapshotService {

	private static final String ALGORITHM = "HmacSHA256";
	private static final byte[] KEY = processKey();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private CurrencySnapshotService() {
	}

	public static String issue(String email, String currency) {
		String emailPart = ENCODER.encodeToString(normalizeEmail(email).getBytes(StandardCharsets.UTF_8));
		String payload = emailPart + "." + normalizeCurrency(currency);
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
		return ENCODER.encodeToString(payloadBytes) + "." + ENCODER.encodeToString(sign(payloadBytes));
	}

	public static Optional<String> resolve(String snapshot, String email) {
		if (snapshot == null || snapshot.isBlank() || email == null || email.isBlank()) return Optional.empty();
		try {
			String[] parts = snapshot.split("\\.", -1);
			if (parts.length != 2) return Optional.empty();
			byte[] payload = DECODER.decode(parts[0]);
			byte[] signature = DECODER.decode(parts[1]);
			if (!MessageDigest.isEqual(signature, sign(payload))) return Optional.empty();
			String[] values = new String(payload, StandardCharsets.UTF_8).split("\\.", -1);
			if (values.length != 2) return Optional.empty();
			String tokenEmail = new String(DECODER.decode(values[0]), StandardCharsets.UTF_8);
			if (!normalizeEmail(email).equals(tokenEmail)) return Optional.empty();
			return Optional.of(normalizeCurrency(values[1]));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	private static byte[] sign(byte[] payload) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(KEY, ALGORITHM));
			return mac.doFinal(payload);
		} catch (java.security.GeneralSecurityException exception) {
			throw new IllegalStateException("HMAC-SHA-256 is unavailable", exception);
		}
	}

	private static byte[] processKey() {
		byte[] key = new byte[32];
		new SecureRandom().nextBytes(key);
		return key;
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizeCurrency(String currency) {
		return Currency.getInstance(currency).getCurrencyCode();
	}
}
