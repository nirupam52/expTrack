<script lang="ts">
	import type { Dashboard } from '$lib/api/types';

	let { title, currencies, currency, selectedCurrency = $bindable(''), onAddExpense }: {
		title: string;
		currencies: Dashboard['currencies'];
		currency: Dashboard['currencies'][number] | null;
		selectedCurrency?: string;
		onAddExpense: () => void;
	} = $props();
</script>

<div class="dashboard-heading">
	<div><p class="dashboard-eyebrow">Monthly view</p><h1 id="dashboard-title">{title}</h1><p class="dashboard-intro">Your spending this month</p></div>
	<div class="dashboard-actions">
		{#if currency}<button aria-label="Add expense from dashboard" class="primary" onclick={onAddExpense}>Add expense</button>{/if}
		{#if currencies.length > 1}
			<label class="currency-picker"><span class="sr-only">Dashboard currency</span><select aria-label={`Dashboard currency: ${selectedCurrency}`} bind:value={selectedCurrency}>{#each currencies as item (item.currency)}<option value={item.currency}>{item.currency}</option>{/each}</select></label>
		{:else if currency}<p class="currency-label">{currency.currency}</p>{/if}
	</div>
</div>

<style>
	.dashboard-heading {
		align-items: start;
		display: flex;
		flex-direction: column;
		gap: 1rem;
		justify-content: space-between;
	}

	.dashboard-actions {
		align-items: center;
		display: flex;
		flex-wrap: wrap;
		gap: .75rem;
	}

	.dashboard-heading h1 {
		color: var(--color-text);
		font-size: clamp(2.3rem, 11vw, 3.6rem);
		font-weight: 700;
		letter-spacing: -.06em;
		line-height: .95;
	}

	.dashboard-intro {
		color: var(--color-muted);
		font-size: .94rem;
		margin-top: .55rem;
	}

	.currency-picker select,
	.currency-label {
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

	.dashboard-heading .currency-label {
		color: #b8cac6;
		margin-bottom: .1rem;
	}

	@media (min-width: 64rem) {
		.dashboard-heading {
			align-items: end;
			flex-direction: row;
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
