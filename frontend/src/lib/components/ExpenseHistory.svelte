<script lang="ts">
	import { untrack } from 'svelte';
	import { get } from '$lib/api/client';
	import { expensePageSchema, type Category, type Expense, type ExpenseHistoryFilters } from '$lib/api/types';
	import { formatCurrency } from '$lib/utils/format-currency';

	let { categories, initialFilters, reloadVersion, onEdit, onDelete, onFiltersChange }: {
		categories: Category[];
		initialFilters: ExpenseHistoryFilters;
		reloadVersion: number;
		onEdit: (expense: Expense) => void;
		onDelete: (expense: Expense) => Promise<boolean>;
		onFiltersChange: (filters: ExpenseHistoryFilters) => void;
	} = $props();

	const filters = untrack(() => ({ ...initialFilters }));
	let query = $state(filters.query);
	let categoryId = $state<number | null>(filters.categoryId);
	let currency = $state(filters.currency);
	let from = $state(filters.from);
	let to = $state(filters.to);
	let applied = $state<ExpenseHistoryFilters>(filters);
	let expenses = $state.raw<Expense[]>([]);
	let nextCursor = $state<string | null>(null);
	let loading = $state(false);
	let error = $state('');
	let confirming = $state<Expense | null>(null);
	let deleting = $state(false);
	let loadVersion = 0;
	let deleteDialog = $state<HTMLDialogElement>();
	let categoryNames = $derived(new Map(categories.map((category) => [category.id, category.name])));
	let filtersActive = $derived(applied.query.length > 0 || applied.categoryId !== null || applied.currency.length > 0 || applied.from.length > 0 || applied.to.length > 0);

	$effect(() => {
		reloadVersion;
		void untrack(() => load(null, false));
	});

	$effect(() => {
		if (!deleteDialog) return;
		if (confirming && !deleteDialog.open) deleteDialog.showModal();
		if (!confirming && deleteDialog.open) deleteDialog.close();
	});

	function historyPath(cursor: string | null) {
		const parameters = new URLSearchParams();
		if (applied.query) parameters.set('query', applied.query);
		if (applied.categoryId !== null) parameters.set('categoryId', String(applied.categoryId));
		if (applied.currency) parameters.set('currency', applied.currency);
		if (applied.from) parameters.set('from', applied.from);
		if (applied.to) parameters.set('to', applied.to);
		if (cursor) parameters.set('cursor', cursor);
		const queryString = parameters.toString();
		return queryString ? `/api/expenses?${queryString}` : '/api/expenses';
	}

	async function load(cursor: string | null, append: boolean) {
		const version = ++loadVersion;
		loading = true;
		error = '';
		if (!append) nextCursor = null;
		try {
			const page = await get(historyPath(cursor), expensePageSchema);
			if (version !== loadVersion) return;
			expenses = append ? [...expenses, ...page.items] : page.items;
			nextCursor = page.nextCursor;
		} catch {
			if (version === loadVersion) error = 'Could not load your expense history. Please try again.';
		} finally {
			if (version === loadVersion) loading = false;
		}
	}

	function applyFilters(event: SubmitEvent) {
		event.preventDefault();
		currency = currency.trim().toUpperCase();
		applied = { query: query.trim(), categoryId, currency, from, to };
		onFiltersChange(applied);
		void load(null, false);
	}

	function clearFilters() {
		query = '';
		categoryId = null;
		currency = '';
		from = '';
		to = '';
		applied = { query: '', categoryId: null, currency: '', from: '', to: '' };
		onFiltersChange(applied);
		void load(null, false);
	}

	async function confirmDelete() {
		if (!confirming) return;
		deleting = true;
		try {
			if (await onDelete(confirming)) confirming = null;
		} finally {
			deleting = false;
		}
	}

	function cancelDelete(event: Event) {
		if (deleting) event.preventDefault();
		else confirming = null;
	}
</script>

<section class="history" aria-labelledby="history-title">
	<div class="heading">
		<div><p class="eyebrow">Your ledger</p><h2 id="history-title">Expense history</h2></div>
		<p>{applied.currency ? `${applied.currency} - ` : ''}{expenses.length} shown</p>
	</div>
	<form class="history-filters" onsubmit={applyFilters}>
		<label class="field search-field">Search title or note<input bind:value={query} maxlength="500" placeholder="Coffee, train fare…" /></label>
		<div class="filter-pair">
		<label class="field">Category<select bind:value={categoryId}><option value={null}>All categories</option>{#each categories as item (item.id)}<option value={item.id}>{item.name}</option>{/each}</select></label>
			<label class="field">Currency<input bind:value={currency} autocapitalize="characters" placeholder="USD" /></label>
			<label class="field">From<input bind:value={from} type="date" max={to || undefined} /></label>
			<label class="field">To<input bind:value={to} type="date" min={from || undefined} /></label>
		</div>
		<div class="filter-actions"><button class="primary" disabled={loading}>Apply filters</button>{#if filtersActive}<button class="quiet" type="button" onclick={clearFilters}>Clear filters</button>{/if}</div>
	</form>
	{#if error}<div class="history-message"><p class="error" role="alert">{error}</p><button class="quiet" onclick={() => void load(null, false)}>Retry</button></div>
	{:else if expenses.length === 0 && !loading}<p class="empty">No expenses match these filters.</p>
	{:else}<ul>{#each expenses as expense (expense.id)}<li class="expense-row"><div><strong>{expense.title}</strong><span>{categoryNames.get(expense.categoryId) ?? 'Unknown category'} · {expense.date}{#if expense.note} · {expense.note}{/if}</span></div><div class="expense-actions"><b>{formatCurrency(expense)}</b><button class="quiet" onclick={() => onEdit(expense)}>Edit</button><button class="danger" onclick={() => confirming = expense}>Delete</button></div></li>{/each}</ul>{/if}
	{#if loading}<p class="status">Loading expenses…</p>{/if}
	{#if nextCursor && !loading && !error}<button class="load-more" onclick={() => void load(nextCursor, true)}>Load more</button>{/if}
</section>

<dialog bind:this={deleteDialog} oncancel={cancelDelete} aria-labelledby="delete-title">
	{#if confirming}<h2 id="delete-title">Delete “{confirming.title}”?</h2><p>This permanently removes the expense. It cannot be undone.</p><div class="dialog-actions"><button class="quiet" onclick={() => confirming = null} disabled={deleting}>Cancel</button><button class="danger destructive" onclick={confirmDelete} disabled={deleting}>{deleting ? 'Deleting…' : 'Delete expense'}</button></div>{/if}
</dialog>
