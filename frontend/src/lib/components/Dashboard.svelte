<script lang="ts">
	import { onMount } from 'svelte';
	import { get } from '$lib/api/client';
	import { dashboardSchema, type Category, type Dashboard } from '$lib/api/types';
	import { formatCurrency } from '$lib/utils/format-currency';

	let { categories, defaultCurrency, onAddExpense, onViewHistory, onViewCategory }: {
		categories: Category[];
		defaultCurrency: string;
		onAddExpense: () => void;
		onViewHistory: () => void;
		onViewCategory: (categoryId: number, month: string) => void;
	} = $props();

	let dashboard = $state.raw<Dashboard | null>(null);
	let selectedCurrency = $state('');
	let loading = $state(false);
	let error = $state('');
	let categoryNames = $derived(new Map(categories.map((category) => [category.id, category.name])));
	let currency = $derived(dashboard?.currencies.find((item) => item.currency === selectedCurrency) ?? null);

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

	function share(amountMinor: string, totalMinor: string) {
		const amount = BigInt(amountMinor);
		const total = BigInt(totalMinor);
		return total === 0n ? 0 : Number((amount * 100n + total / 2n) / total);
	}

	function monthName(month: string) {
		return new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric', timeZone: 'UTC' })
			.format(new Date(`${month}-01T00:00:00Z`));
	}
</script>

<section class="dashboard" aria-labelledby="dashboard-title">
	{#if loading && !dashboard}
		<p class="status">Loading your dashboardâ€¦</p>
	{:else if error && !dashboard}
		<div class="dashboard-message"><p class="error" role="alert">{error}</p><button class="primary" onclick={() => void load()}>Retry</button></div>
	{:else if dashboard}
		{@const month = dashboard.month}
		<div class="dashboard-heading">
			<div><p class="eyebrow">Monthly view</p><h1 id="dashboard-title">{monthName(month)}</h1><p class="intro">Your spending this month</p></div>
			{#if dashboard.currencies.length > 1}
				<label class="currency-picker"><span class="sr-only">Dashboard currency</span><select bind:value={selectedCurrency}>{#each dashboard.currencies as item (item.currency)}<option value={item.currency}>{item.currency}</option>{/each}</select></label>
			{:else if currency}<p class="currency-label">{currency.currency}</p>{/if}
		</div>

		{#if currency}
			<section class="month-total" aria-label={`Total spending in ${currency.currency}`}>
				<p>Spent this month</p><strong>{formatCurrency({ amountMinor: currency.totalMinor, currency: currency.currency })}</strong><span>Across {currency.categories.length} {currency.categories.length === 1 ? 'category' : 'categories'}</span>
			</section>

			<section class="breakdown" aria-labelledby="breakdown-title">
				<h2 id="breakdown-title">Where it went</h2>
				<div class="spending-ribbon" aria-label="Category shares">
					{#each currency.categories as item (item.categoryId)}<span style:--share={`${share(item.amountMinor, currency.totalMinor)}%`}></span>{/each}
				</div>
				<ul class="category-list">
					{#each currency.categories as item (item.categoryId)}
						<li><button onclick={() => onViewCategory(item.categoryId, month)}><span>{categoryNames.get(item.categoryId) ?? 'Unknown category'} <b aria-hidden="true">â€º</b></span><strong>{formatCurrency({ amountMinor: item.amountMinor, currency: currency.currency })}</strong><em>{share(item.amountMinor, currency.totalMinor)}%</em></button></li>
					{/each}
				</ul>
			</section>
		{:else}
			<section class="dashboard-empty" aria-labelledby="empty-dashboard-title"><p class="eyebrow">Ready when you are</p><h2 id="empty-dashboard-title">Start adding expenses to see insights here.</h2><button class="primary" onclick={onAddExpense}>Add an expense</button></section>
		{/if}

		<section class="recent-expenses" aria-labelledby="recent-title">
			<div class="section-heading"><h2 id="recent-title">Latest expenses</h2><button class="quiet link-button" onclick={onViewHistory}>See all <span aria-hidden="true">â€º</span></button></div>
			{#if dashboard.recentExpenses.length}
				<ul>{#each dashboard.recentExpenses as expense (expense.id)}<li><div><strong>{expense.title}</strong><span>{categoryNames.get(expense.categoryId) ?? 'Unknown category'} Â· {expense.date}</span></div><b>{formatCurrency(expense)}</b></li>{/each}</ul>
			{:else}<p class="empty">No expenses yet.</p>{/if}
		</section>
	{/if}
</section>

<style>
	.dashboard { display: grid; gap: 2rem; padding: 1.5rem 0 6.5rem; }
	.dashboard-heading, .section-heading { align-items: end; display: flex; justify-content: space-between; gap: 1rem; }
	.dashboard h1, .dashboard h2, .dashboard p { margin: 0; }
	.dashboard h1 { color: #f5f8f6; font-family: ui-serif, Georgia, serif; font-size: clamp(2.3rem, 11vw, 3.6rem); font-weight: 700; letter-spacing: -.06em; line-height: .95; }
	.dashboard h2 { color: #f5f8f6; font-size: 1.1rem; letter-spacing: -.025em; }
	.dashboard .eyebrow { color: #70cbaa; font-size: .69rem; font-weight: 800; letter-spacing: .14em; margin-bottom: .45rem; text-transform: uppercase; }
	.intro { color: #a7bbb6; font-size: .94rem; margin-top: .55rem !important; }
	.currency-picker select, .currency-label { background: #172927; border: 1px solid #45625c; border-radius: .35rem; color: #8cafFF; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .82rem; font-weight: 800; letter-spacing: .04em; min-height: 2.55rem; padding: .55rem 2rem .55rem .75rem; }
	.currency-label { border-color: #45625c; color: #b8cac6; margin-bottom: .1rem !important; }
	.month-total { background: #203734; border: 1px solid #54d2a0; display: grid; gap: .55rem; padding: 1.25rem; }
	.month-total p { color: #d6e5e0; font-size: .71rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; }
	.month-total strong { color: #fff; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: clamp(2rem, 9vw, 3rem); letter-spacing: -.07em; }
	.month-total span { color: #c5d6d1; font-size: .85rem; font-weight: 700; }
	.breakdown { display: grid; gap: 1rem; }
	.spending-ribbon { display: flex; height: .65rem; overflow: hidden; }
	.spending-ribbon span { background: #54d2a0; flex: 0 0 var(--share); min-width: 1px; }
	.spending-ribbon span:nth-child(2) { background: #72a7ff; }
	.spending-ribbon span:nth-child(3) { background: #f3a553; }
	.spending-ribbon span:nth-child(4) { background: #de8ee8; }
	.spending-ribbon span:nth-child(n + 5) { background: #a7bbb6; }
	.category-list, .recent-expenses ul { list-style: none; margin: 0; padding: 0; }
	.category-list { display: grid; gap: .12rem; }
	.category-list button { align-items: center; background: transparent; border: 0; color: #f5f8f6; cursor: pointer; display: grid; font: inherit; gap: .75rem; grid-template-columns: 1fr auto auto; padding: .55rem 0; text-align: left; width: 100%; }
	.category-list button:hover span, .category-list button:focus-visible span { color: #70cbaa; }
	.category-list span { font-size: .9rem; font-weight: 700; }
	.category-list span b { color: #72a7ff; font-size: 1.35rem; line-height: .5; margin-left: .15rem; }
	.category-list strong, .category-list em, .recent-expenses b { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .84rem; font-style: normal; font-variant-numeric: tabular-nums; white-space: nowrap; }
	.category-list em { color: #b8cac6; min-width: 2.25rem; text-align: right; }
	.recent-expenses { border-top: 1px solid #334b47; display: grid; gap: .9rem; padding-top: 1.25rem; }
	.link-button { align-items: center; display: inline-flex; gap: .2rem; }
	.recent-expenses ul { display: grid; gap: .45rem; }
	.recent-expenses li { align-items: center; background: #172725; border: 1px solid #3a5751; display: flex; gap: 1rem; justify-content: space-between; padding: .75rem .8rem; }
	.recent-expenses li div { min-width: 0; }
	.recent-expenses li strong, .recent-expenses li span { display: block; }
	.recent-expenses li strong { font-size: .9rem; }
	.recent-expenses li span { color: #a7bbb6; font-size: .75rem; margin-top: .2rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.dashboard-empty, .dashboard-message { border: 1px solid #3a5751; display: grid; gap: 1rem; padding: 1.25rem; }
	.dashboard-empty h2 { font-family: ui-serif, Georgia, serif; font-size: 1.5rem; line-height: 1.1; }
	.dashboard-empty .primary, .dashboard-message .primary { justify-self: start; }
	.empty { color: #a7bbb6; font-size: .9rem; }
	.sr-only { clip: rect(0, 0, 0, 0); clip-path: inset(50%); height: 1px; overflow: hidden; position: absolute; white-space: nowrap; width: 1px; }
</style>
