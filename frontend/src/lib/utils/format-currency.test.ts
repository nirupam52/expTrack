import assert from 'node:assert/strict';
import test from 'node:test';

import { formatCurrency } from './format-currency.ts';

const digitMap = new Map(Array.from(new Intl.NumberFormat(undefined, { useGrouping: false }).format(9876543210), (digit, index) => [digit, String(9 - index)]));

function digits(value: string) {
	return Array.from(value, (character) => digitMap.get(character) ?? character).filter((character) => /\d/.test(character)).join('');
}

function fraction(value: string, currency: string) {
	const decimal = new Intl.NumberFormat(undefined, { style: 'currency', currency }).formatToParts(0).find((part) => part.type === 'decimal')?.value;
	return decimal === undefined ? undefined : digits(value.slice(value.lastIndexOf(decimal) + decimal.length));
}

test('formats exact minor units without rounding', () => {
	const minorUnits = '9223372036854775807';
	const usd = formatCurrency({ amountMinor: minorUnits, currency: 'USD' });
	const jpy = formatCurrency({ amountMinor: minorUnits, currency: 'JPY' });

	assert.equal(digits(usd), minorUnits);
	assert.equal(fraction(usd, 'USD'), '07');
	assert.equal(digits(jpy), minorUnits);
	assert.equal(fraction(jpy, 'JPY'), undefined);
});
