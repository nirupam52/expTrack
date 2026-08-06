import type { Expense } from '$lib/api/types';

export function formatCurrency({ amountMinor, currency }: Pick<Expense, 'amountMinor' | 'currency'>) {
	const formatter = new Intl.NumberFormat(undefined, { style: 'currency', currency });
	const fractionDigits = formatter.resolvedOptions().maximumFractionDigits ?? 2;
	const amount = BigInt(amountMinor);
	if (fractionDigits === 0) return formatter.format(amount);
	const scale = 10n ** BigInt(fractionDigits);
	const whole = amount / scale;
	const fraction = amount % scale;
	const integerFormatter = new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 });
	const fractionFormatter = new Intl.NumberFormat(undefined, { minimumIntegerDigits: fractionDigits, useGrouping: false });

	return formatter.formatToParts(0).map((part) => {
		if (part.type === 'integer') return integerFormatter.format(whole);
		if (part.type === 'fraction') return fractionFormatter.format(fraction);
		return part.value;
	}).join('');
}
