package com.exptrack.currency;

import com.exptrack.currency.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyServiceTest {

	private final CurrencyService currencies = new CurrencyService();

	@Test
	void validateAcceptsAKnownIsoCodeAndReturnsItNormalized() {
		assertThat(currencies.validate("USD")).isEqualTo("USD");
	}

	@Test
	void validateRejectsAnUnknownCode() {
		assertThatThrownBy(() -> currencies.validate("not-a-currency"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(exception -> {
					ResponseStatusException error = (ResponseStatusException) exception;
					assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(error.getReason()).isEqualTo("Currency is invalid");
				});
	}

	@Test
	void validateRejectsANullCode() {
		assertThatThrownBy(() -> currencies.validate(null)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void minorUnitsConvertsAUsdAmountToCents() {
		assertThat(currencies.minorUnits("12.34", "USD")).isEqualTo(1234L);
	}

	@Test
	void minorUnitsConvertsAJpyAmountWithNoFractionalUnit() {
		assertThat(currencies.minorUnits("500", "JPY")).isEqualTo(500L);
	}

	@Test
	void minorUnitsConvertsAThreeDecimalBhdAmount() {
		assertThat(currencies.minorUnits("12.345", "BHD")).isEqualTo(12345L);
	}

	@Test
	void minorUnitsRejectsMorePrecisionThanTheCurrencyAllows() {
		assertThatThrownBy(() -> currencies.minorUnits("12.345", "USD"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason()).isEqualTo("Amount is invalid"));
	}

	@Test
	void minorUnitsRejectsZeroAndNegativeAmounts() {
		assertThatThrownBy(() -> currencies.minorUnits("0", "USD")).isInstanceOf(ResponseStatusException.class);
		assertThatThrownBy(() -> currencies.minorUnits("-1", "USD")).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void minorUnitsRejectsNonNumericAmounts() {
		assertThatThrownBy(() -> currencies.minorUnits("abc", "USD")).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void minorUnitsReportsTheCurrencyAsInvalidRatherThanTheAmountWhenBothAreBad() {
		assertThatThrownBy(() -> currencies.minorUnits("12.34", "not-a-currency"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(exception -> assertThat(((ResponseStatusException) exception).getReason()).isEqualTo("Currency is invalid"));
	}
}
