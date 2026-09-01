<script lang="ts">
	import type { Expense } from '$lib/api/types';
	import { formatCurrency } from '$lib/utils/format-currency';

	let { expenses, names, onViewHistory }: {
		expenses: Expense[];
		names: Map<number, string>;
		onViewHistory: () => void;
	} = $props();
</script>

<section class="recent-expenses" aria-labelledby="recent-title">
	<div class="section-heading"><h2 id="recent-title">Latest expenses</h2><button aria-label="View all expense history" class="quiet link-button" onclick={onViewHistory}>See all <span aria-hidden="true">&gt;</span></button></div>
	{#if expenses.length}
		<ul>{#each expenses as expense (expense.id)}<li><div><strong>{expense.title}</strong><span>{names.get(expense.categoryId) ?? 'Unknown category'} - {expense.date}</span></div><b>{formatCurrency(expense)}</b></li>{/each}</ul>
	{:else}<p class="dashboard-empty-copy">No expenses yet.</p>{/if}
</section>

<style>
	.section-heading {
		align-items: end;
		display: flex;
		gap: 1rem;
		justify-content: space-between;
	}

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
		list-style: none;
		margin: 0;
		padding: 0;
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

	.recent-expenses b {
		font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
		font-size: .84rem;
		font-variant-numeric: tabular-nums;
		white-space: nowrap;
	}

	.dashboard-empty-copy {
		color: var(--color-muted);
		font-size: .9rem;
		line-height: 1.5;
	}

	@media (min-width: 64rem) {
		:global(.dashboard-overview) .recent-expenses {
			border-left: 1px solid var(--color-divider);
			border-top: 0;
			padding: 0 0 0 2rem;
		}
	}
</style>
