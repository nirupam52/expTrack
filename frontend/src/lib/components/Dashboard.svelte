<script lang="ts">
	import { onMount } from 'svelte';
	import { get } from '$lib/api/client';
	import { dashboardSchema, type Category, type Dashboard } from '$lib/api/types';
	import { categoryShares } from '$lib/utils/category-shares';
	import CategoryBreakdown from './dashboard/CategoryBreakdown.svelte';
	import DashboardHeading from './dashboard/DashboardHeading.svelte';
	import MonthTotal from './dashboard/MonthTotal.svelte';
	import RecentExpenses from './dashboard/RecentExpenses.svelte';

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
		<DashboardHeading currency={currency} currencies={dashboard.currencies} title={monthName(month)} bind:selectedCurrency onAddExpense={onAddExpense} />
		<div class={currency ? 'dashboard-overview' : 'dashboard-content'}>
			{#if currency}
				<div class="dashboard-summary">
					<MonthTotal categoryCount={currency.categories.length} currencyLabel={currency.currency} total={currency.totalMinor} />
					<CategoryBreakdown currencyLabel={currency.currency} month={month} monthLabel={monthName(month)} names={categoryNames} shares={selectedCategoryShares} onViewCategory={onViewCategory} />
				</div>
			{:else if dashboard.recentExpenses.length === 0}
				<section class="dashboard-empty" aria-labelledby="empty-dashboard-title"><p class="dashboard-eyebrow">Ready when you are</p><h2 id="empty-dashboard-title">Start adding expenses to see insights here.</h2><button class="primary" onclick={onAddExpense}>Add an expense</button></section>
			{:else}
				<section class="dashboard-empty" aria-labelledby="empty-dashboard-title"><p class="dashboard-eyebrow">This month</p><h2 id="empty-dashboard-title">No spending this month yet.</h2><button class="primary" onclick={onAddExpense}>Add an expense</button></section>
			{/if}

			<RecentExpenses expenses={dashboard.recentExpenses} names={categoryNames} onViewHistory={onViewHistory} />
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

	.dashboard-content,
	.dashboard-overview,
	.dashboard-summary {
		display: grid;
		gap: 2rem;
	}

	/* Typography (h1/h2/p and eyebrow live in child components) */
	.dashboard :global(h1),
	.dashboard :global(h2),
	.dashboard :global(p) {
		margin: 0;
	}

	.dashboard :global(h2) {
		color: var(--color-text);
		font-size: 1.1rem;
		letter-spacing: -.025em;
	}

	.dashboard :global(.dashboard-eyebrow) {
		color: var(--color-accent);
		font-size: .69rem;
		font-weight: 800;
		letter-spacing: .14em;
		margin-bottom: .45rem;
		text-transform: uppercase;
	}

	/* Empty and error states */
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

	@media (min-width: 64rem) {
		.dashboard-overview {
			align-items: start;
			grid-template-columns: minmax(0, 1fr) minmax(20rem, .85fr);
		}
	}
</style>
