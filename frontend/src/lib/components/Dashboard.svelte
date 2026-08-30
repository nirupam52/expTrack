<script lang="ts">
	import { onMount } from 'svelte';
	import { get } from '$lib/api/client';
	import { dashboardSchema, type Category, type Dashboard } from '$lib/api/types';
	import { categoryShares } from '$lib/utils/category-shares';
	import { formatCurrency } from '$lib/utils/format-currency';

	let { categories, defaultCurrency, onAddExpense, onViewHistory, onViewCategory }: {
		categories: Category[];
		defaultCurrency: string;
		onAddExpense: () => void;
		onViewHistory: () => void;
		onViewCategory: (categoryId: number, month: string, currency: string) => void;
	} = $props();

	let dashboard = $state.raw<Dashboard | null>(null);
	let selectedCurrency = $state('');
	let loading = $state(false);
	let error = $state('');
	let categoryNames = $derived(new Map(categories.map((category) => [category.id, category.name])));
	let currency = $derived(dashboard?.currencies.find((item) => item.currency === selectedCurrency) ?? null);
	let selectedCategoryShares = $derived.by(() => currency ? categoryRows(currency.categories) : []);

	onMount(() => void load());

	async function load() {
		loading = true;
		error = '';
		try {
			const response = await get('/api/expenses/dashboard', dashboardSchema);
			dashboard = response;
			if (!response.currencies.some((item) => item.currency === selectedCurrency)) {
				selectedCurrency = response.currencies.find((item) => item.currency === defaultCurrency)?.currency ?? response.currencies[0]?.currency ?? '';
			}
		} catch {
			error = 'Could not load your dashboard. Please try again.';
		} finally {
			loading = false;
		}
	}

	function categoryRows(categories: { categoryId: number; amountMinor: string }[]) {
		const shares = categoryShares(categories.map((item) => item.amountMinor));
		return categories.map((category, index) => ({ category, share: shares[index] }));
	}

	function monthName(month: string) {
		return new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric', timeZone: 'UTC' })
			.format(new Date(`${month}-01T00:00:00Z`));
	}
</script>

<section class="dashboard" aria-labelledby="dashboard-title">
	{#if loading && !dashboard}
		<p class="status">Loading your dashboard...</p>
	{:else if error && !dashboard}
		<div class="dashboard-message"><p class="error" role="alert">{error}</p><button class="primary" onclick={() => void load()}>Retry</button></div>
	{:else if dashboard}
		{@const month = dashboard.month}
		<div class="dashboard-heading">
			<div><p class="dashboard-eyebrow">Monthly view</p><h1 id="dashboard-title">{monthName(month)}</h1><p class="dashboard-intro">Your spending this month</p></div>
			<div class="dashboard-actions">
				{#if currency}<button aria-label="Add expense from dashboard" class="primary" onclick={onAddExpense}>Add expense</button>{/if}
				{#if dashboard.currencies.length > 1}
					<label class="currency-picker"><span class="sr-only">Dashboard currency</span><select aria-label={`Dashboard currency: ${selectedCurrency}`} bind:value={selectedCurrency}>{#each dashboard.currencies as item (item.currency)}<option value={item.currency}>{item.currency}</option>{/each}</select></label>
				{:else if currency}<p class="currency-label">{currency.currency}</p>{/if}
			</div>
		</div>

		<div class={currency ? 'dashboard-overview' : 'dashboard-content'}>
			{#if currency}
				<div class="dashboard-summary">
					<section class="month-total" aria-label={`Total spending in ${currency.currency}`}>
						<p>Spent this month</p><strong>{formatCurrency({ amountMinor: currency.totalMinor, currency: currency.currency })}</strong><span>Across {currency.categories.length} {currency.categories.length === 1 ? 'category' : 'categories'}</span>
					</section>

					<section class="breakdown" aria-labelledby="breakdown-title">
						<h2 id="breakdown-title">Where it went</h2>
						<div class="spending-ribbon" aria-label="Category shares">
							{#each selectedCategoryShares as item (item.category.categoryId)}<span style:--amount={item.category.amountMinor}></span>{/each}
						</div>
						<ul class="category-list">
							{#each selectedCategoryShares as item (item.category.categoryId)}
								<li><button aria-label={`View ${categoryNames.get(item.category.categoryId) ?? 'Unknown category'} expenses for ${monthName(month)} in ${currency.currency}`} onclick={() => onViewCategory(item.category.categoryId, month, currency.currency)}><span>{categoryNames.get(item.category.categoryId) ?? 'Unknown category'} <b aria-hidden="true">&gt;</b></span><strong>{formatCurrency({ amountMinor: item.category.amountMinor, currency: currency.currency })}</strong><em>{item.share}%</em></button></li>
							{/each}
						</ul>
					</section>
				</div>
			{:else}
				<section class="dashboard-empty" aria-labelledby="empty-dashboard-title"><p class="dashboard-eyebrow">Ready when you are</p><h2 id="empty-dashboard-title">Start adding expenses to see insights here.</h2><button class="primary" onclick={onAddExpense}>Add an expense</button></section>
			{/if}

			<section class="recent-expenses" aria-labelledby="recent-title">
				<div class="section-heading"><h2 id="recent-title">Latest expenses</h2><button aria-label="View all expense history" class="quiet link-button" onclick={onViewHistory}>See all <span aria-hidden="true">&gt;</span></button></div>
				{#if dashboard.recentExpenses.length}
					<ul>{#each dashboard.recentExpenses as expense (expense.id)}<li><div><strong>{expense.title}</strong><span>{categoryNames.get(expense.categoryId) ?? 'Unknown category'} - {expense.date}</span></div><b>{formatCurrency(expense)}</b></li>{/each}</ul>
				{:else}<p class="dashboard-empty-copy">No expenses yet.</p>{/if}
			</section>
		</div>
	{/if}
</section>

<style>
	/* Layout */
	.dashboard {
		display: grid;
		gap: 2rem;
		padding: 1.5rem 0;
	}

	.dashboard-heading,
	.section-heading {
		align-items: end;
		display: flex;
		gap: 1rem;
		justify-content: space-between;
	}

	.dashboard-heading {
		align-items: start;
		flex-direction: column;
	}

	.dashboard-actions {
		align-items: center;
		display: flex;
		flex-wrap: wrap;
		gap: .75rem;
	}

	.dashboard-content,
	.dashboard-overview,
	.dashboard-summary {
		display: grid;
		gap: 2rem;
	}

	/* Typography */
	.dashboard h1,
	.dashboard h2,
	.dashboard p {
		margin: 0;
	}

	.dashboard h1 {
		color: var(--color-text);
		font-size: clamp(2.3rem, 11vw, 3.6rem);
		font-weight: 700;
		letter-spacing: -.06em;
		line-height: .95;
	}

	.dashboard h2 {
		color: var(--color-text);
		font-size: 1.1rem;
		letter-spacing: -.025em;
	}

	.dashboard .dashboard-eyebrow {
		color: var(--color-accent);
		font-size: .69rem;
		font-weight: 800;
		letter-spacing: .14em;
		margin-bottom: .45rem;
		text-transform: uppercase;
	}

	.dashboard .dashboard-intro {
		color: var(--color-muted);
		font-size: .94rem;
		margin-top: .55rem;
	}

	/* Currency and total */
	.currency-picker select,
	.dashboard .currency-label {
		background: #172927;
		border: 1px solid var(--color-border-strong);
		border-radius: .35rem;
		color: var(--color-link);
		font-size: .82rem;
		font-weight: 800;
		letter-spacing: .04em;
		min-height: 2.75rem;
		padding: .55rem 2rem .55rem .75rem;
	}

	.dashboard .currency-label {
		color: #b8cac6;
		margin-bottom: .1rem;
	}

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

	/* Category breakdown */
	.breakdown {
		display: grid;
		gap: 1rem;
	}

	.spending-ribbon {
		display: flex;
		height: .65rem;
		overflow: hidden;
	}

	.spending-ribbon span {
		background: var(--color-primary);
		flex: var(--amount) 1 0;
	}

	.spending-ribbon span:nth-child(2) {
		background: var(--color-focus);
	}

	.spending-ribbon span:nth-child(3) {
		background: #f3a553;
	}

	.spending-ribbon span:nth-child(4) {
		background: #de8ee8;
	}

	.spending-ribbon span:nth-child(n + 5) {
		background: var(--color-muted);
	}

	.category-list,
	.recent-expenses ul {
		list-style: none;
		margin: 0;
		padding: 0;
	}

	.category-list {
		display: grid;
		gap: .12rem;
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
		font-size: .9rem;
		font-weight: 700;
	}

	.category-list span b {
		color: var(--color-focus);
		font-size: 1.35rem;
		line-height: .5;
		margin-left: .15rem;
	}

	.category-list strong,
	.category-list em,
	.recent-expenses b {
		font-size: .84rem;
		font-style: normal;
		font-variant-numeric: tabular-nums;
		white-space: nowrap;
	}

	.category-list strong,
	.recent-expenses b {
		font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
	}

	.category-list em {
		color: #b8cac6;
		min-width: 2.25rem;
		text-align: right;
	}

	/* Recent expenses and states */
	.recent-expenses {
		border-top: 1px solid var(--color-divider);
		display: grid;
		gap: .9rem;
		padding-top: 1.25rem;
	}

	.link-button {
		align-items: center;
		display: inline-flex;
		gap: .2rem;
		min-height: 2.75rem;
	}

	.recent-expenses ul {
		display: grid;
		gap: .45rem;
	}

	.recent-expenses li {
		align-items: center;
		background: var(--color-surface);
		border: 1px solid var(--color-border);
		display: flex;
		gap: 1rem;
		justify-content: space-between;
		padding: .75rem .8rem;
	}

	.recent-expenses li div {
		min-width: 0;
	}

	.recent-expenses li strong,
	.recent-expenses li span {
		display: block;
	}

	.recent-expenses li strong {
		font-size: .9rem;
	}

	.recent-expenses li span {
		color: var(--color-muted);
		font-size: .75rem;
		margin-top: .2rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.dashboard-empty,
	.dashboard-message {
		border: 1px solid var(--color-border);
		display: grid;
		gap: 1rem;
		padding: 1.25rem;
	}

	.dashboard-empty h2 {
		font-size: 1.5rem;
		line-height: 1.1;
	}

	.dashboard-empty .primary,
	.dashboard-message .primary {
		justify-self: start;
	}

	.dashboard .dashboard-empty-copy {
		color: var(--color-muted);
		font-size: .9rem;
		line-height: 1.5;
	}

	@media (min-width: 64rem) {
		.dashboard-heading {
			align-items: end;
			flex-direction: row;
		}

		.dashboard-overview {
			align-items: start;
			grid-template-columns: minmax(0, 1fr) minmax(20rem, .85fr);
		}

		.dashboard-overview .recent-expenses {
			border-left: 1px solid var(--color-divider);
			border-top: 0;
			padding: 0 0 0 2rem;
		}
	}

	/* Accessibility */
	.sr-only {
		clip: rect(0, 0, 0, 0);
		clip-path: inset(50%);
		height: 1px;
		overflow: hidden;
		position: absolute;
		white-space: nowrap;
		width: 1px;
	}
</style>
