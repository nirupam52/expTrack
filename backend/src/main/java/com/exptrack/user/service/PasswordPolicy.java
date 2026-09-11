package com.exptrack.user.service;

/**
 * NIST SP 800-63B single-factor length rule, measured in Unicode code points with no composition rule.
 * Common-password and breach blocklist validation is deferred and must stay out of this module.
 */
public final class PasswordPolicy {

	public static final int MIN_CODE_POINTS = 15;
	public static final int MAX_CODE_POINTS = 64;

	private PasswordPolicy() {
	}

	public static boolean accepts(String password) {
		if (password == null) {
			return false;
		}
		int codePoints = password.codePointCount(0, password.length());
		return codePoints >= MIN_CODE_POINTS && codePoints <= MAX_CODE_POINTS;
	}
}
