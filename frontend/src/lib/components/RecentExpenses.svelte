<script lang="ts">
	import type { Category, Expense } from '$lib/api/types';
	import { formatCurrency } from '$lib/utils/format-currency';

	let { expenses, categories }: { expenses: Expense[]; categories: Category[] } = $props();

	let categoryNames = $derived(new Map(categories.map((category) => [category.id, category.name])));
</script>

<section class="recent" aria-labelledby="recent-title">
	<div class="heading"><div><p class="eyebrow">Latest entries</p><h2 id="recent-title">Recently added</h2></div><p>{expenses.length}</p></div>
	{#if expenses.length === 0}<p class="empty">Your first saved expense will appear here.</p>
	{:else}<ul>{#each expenses as expense (expense.id)}<li><div><strong>{expense.title}</strong><span>{categoryNames.get(expense.categoryId) ?? 'Unknown category'} · {expense.date}{#if expense.note} · {expense.note}{/if}</span></div><b>{formatCurrency(expense)}</b></li>{/each}</ul>{/if}
</section>
