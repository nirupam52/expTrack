package com.exptrack.currency.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * The single seam for the ISO 4217 currency rules the money boundary (ADR-015) depends on:
 * validating a currency code and converting a display amount to that currency's minor units.
 */
@Service
public class CurrencyService {

	public String validate(String code) {
		return currency(code).getCurrencyCode();
	}

	public long minorUnits(String amount, String currencyCode) {
		int fractionDigits = currency(currencyCode).getDefaultFractionDigits();
		try {
			BigDecimal value = new BigDecimal(amount.trim());
			if (fractionDigits < 0 || value.signum() <= 0 || !amount.trim().matches("\\d+(?:\\.\\d+)?")) {
				throw new ArithmeticException();
			}
			return value.setScale(fractionDigits, RoundingMode.UNNECESSARY).movePointRight(fractionDigits).longValueExact();
		} catch (IllegalArgumentException | ArithmeticException exception) {
			throw invalid("Amount is invalid");
		}
	}

	private Currency currency(String code) {
		try {
			return Currency.getInstance(code);
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw invalid("Currency is invalid");
		}
	}

	private ResponseStatusException invalid(String detail) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
	}
}
