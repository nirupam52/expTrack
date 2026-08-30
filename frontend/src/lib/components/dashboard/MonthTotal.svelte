<script lang="ts">
	import { formatCurrency } from '$lib/utils/format-currency';

	let { total, currencyLabel, categoryCount }: {
		total: string;
		currencyLabel: string;
		categoryCount: number;
	} = $props();
</script>

<section class="month-total" aria-label={`Total spending in ${currencyLabel}`}>
	<p>Spent this month</p><strong>{formatCurrency({ amountMinor: total, currency: currencyLabel })}</strong><span>Across {categoryCount} {categoryCount === 1 ? 'category' : 'categories'}</span>
</section>

<style>
	.month-total {
		background: var(--color-surface-raised);
		border: 1px solid var(--color-primary);
		display: grid;
		gap: .55rem;
		padding: 1.25rem;
	}

	.month-total p {
		color: #d6e5e0;
		font-size: .71rem;
		font-weight: 800;
		letter-spacing: .09em;
		text-transform: uppercase;
	}

	.month-total strong {
		color: var(--color-white);
		font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
		font-size: clamp(2rem, 9vw, 3rem);
		letter-spacing: -.07em;
	}

	.month-total span {
		color: var(--color-label);
		font-size: .85rem;
		font-weight: 700;
	}
</style>
