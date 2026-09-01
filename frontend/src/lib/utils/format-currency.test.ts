import { expect, test } from 'vitest';

import { amountForInput, formatCurrency } from './format-currency.ts';

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

	expect(digits(usd)).toBe(minorUnits);
	expect(fraction(usd, 'USD')).toBe('07');
	expect(digits(jpy)).toBe(minorUnits);
	expect(fraction(jpy, 'JPY')).toBeUndefined();
});

test('converts exact minor units to editable decimal input', () => {
	expect(amountForInput({ amountMinor: '9223372036854775807', currency: 'USD' })).toBe('92233720368547758.07');
	expect(amountForInput({ amountMinor: '12345', currency: 'BHD' })).toBe('12.345');
	expect(amountForInput({ amountMinor: '500', currency: 'JPY' })).toBe('500');
});
