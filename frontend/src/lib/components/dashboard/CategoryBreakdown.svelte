<script lang="ts">
	import { formatCurrency } from '$lib/utils/format-currency';

	type CategoryRow = { category: { categoryId: number; amountMinor: string }; share: number };

	let { month, monthLabel, currencyLabel, names, shares, onViewCategory }: {
		month: string;
		monthLabel: string;
		currencyLabel: string;
		names: Map<number, string>;
		shares: CategoryRow[];
		onViewCategory: (categoryId: number, month: string, currency: string) => void;
	} = $props();
</script>

<section class="breakdown" aria-labelledby="breakdown-title">
	<h2 id="breakdown-title">Where it went</h2>
	<div class="spending-ribbon" aria-label="Category shares">
		{#each shares as item (item.category.categoryId)}<span style:--amount={item.category.amountMinor}></span>{/each}
	</div>
	<ul class="category-list">
		{#each shares as item (item.category.categoryId)}
			<li><button aria-label={`View ${names.get(item.category.categoryId) ?? 'Unknown category'} expenses for ${monthLabel} in ${currencyLabel}`} onclick={() => onViewCategory(item.category.categoryId, month, currencyLabel)}><span>{names.get(item.category.categoryId) ?? 'Unknown category'} <b aria-hidden="true">&gt;</b></span><strong>{formatCurrency({ amountMinor: item.category.amountMinor, currency: currencyLabel })}</strong><em>{item.share}%</em></button></li>
		{/each}
	</ul>
</section>

<style>
	.breakdown {
		display: grid;
		gap: 1rem;
	}

	.spending-ribbon {
		display: flex;
		height: .65rem;
		overflow: hidden;
	}

	.spending-ribbon span,
	.category-list li {
		--category-color: var(--color-primary);
	}

	.spending-ribbon span {
		background: var(--category-color);
		flex: var(--amount) 1 0;
	}

	.spending-ribbon span:nth-child(2),
	.category-list li:nth-child(2) {
		--category-color: var(--color-focus);
	}

	.spending-ribbon span:nth-child(3),
	.category-list li:nth-child(3) {
		--category-color: #f3a553;
	}

	.spending-ribbon span:nth-child(4),
	.category-list li:nth-child(4) {
		--category-color: #de8ee8;
	}

	.spending-ribbon span:nth-child(n + 5),
	.category-list li:nth-child(n + 5) {
		--category-color: var(--color-muted);
	}

	.category-list {
		display: grid;
		gap: .12rem;
		list-style: none;
		margin: 0;
		padding: 0;
	}

	.category-list button {
		align-items: center;
		background: transparent;
		border: 0;
		color: var(--color-text);
		cursor: pointer;
		display: grid;
		font: inherit;
		gap: .75rem;
		grid-template-columns: 1fr auto auto;
		min-height: 2.75rem;
		padding: .55rem 0;
		text-align: left;
		width: 100%;
	}

	.category-list button:hover span,
	.category-list button:focus-visible span {
		color: var(--color-accent);
	}

	.category-list span {
		align-items: center;
		display: flex;
		font-size: .9rem;
		font-weight: 700;
		gap: .4rem;
	}

	.category-list span::before {
		background: var(--category-color);
		content: '';
		flex: 0 0 .55rem;
		height: .55rem;
	}

	.category-list span b {
		color: var(--color-focus);
		font-size: 1.35rem;
		line-height: .5;
		margin-left: .15rem;
	}

	.category-list strong,
	.category-list em {
		font-size: .84rem;
		font-style: normal;
		font-variant-numeric: tabular-nums;
		white-space: nowrap;
	}

	.category-list strong {
		font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
	}

	.category-list em {
		color: #b8cac6;
		min-width: 2.25rem;
		text-align: right;
	}
</style>
