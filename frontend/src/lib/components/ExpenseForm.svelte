<script lang="ts">
	import { untrack } from 'svelte';
	import type { Category, ExpenseDraft } from '$lib/api/types';
	import { today } from '$lib/utils/date';

	let { categories, initial, submitting, error, submitLabel = 'Save expense', onCancel, onSubmit }: {
		categories: Category[];
		initial: ExpenseDraft;
		submitting: boolean;
		error: string;
		submitLabel?: string;
		onCancel?: () => void;
		onSubmit: (draft: ExpenseDraft) => Promise<boolean>;
	} = $props();

	let title = $state('');
	let amount = $state('');
	let category = $state<number | null>(null);
	let date = $state('');
	let note = $state('');

	$effect.pre(() => {
		untrack(() => {
			title = initial.title;
			amount = initial.amount;
			category = initial.categoryId;
			date = initial.date;
			note = initial.note;
		});
	});

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		const saved = await onSubmit({ title, amount, categoryId: category, date, note });
		if (!saved) return;
		if (onCancel) {
			onCancel();
			return;
		}
		title = '';
		amount = '';
		note = '';
		date = today();
	}
</script>

<form class="form-stack" onsubmit={submit}>
	<label class="field">
		What was it?
		<input bind:value={title} maxlength="120" placeholder="Coffee" autocomplete="off" required />
	</label>
	<div class="form-pair">
		<label class="field">
			Amount
			<input bind:value={amount} inputmode="decimal" placeholder="12.34" required />
		</label>
		<label class="field">
			Category
			<select bind:value={category} required>
				{#each categories as item (item.id)}<option value={item.id}>{item.name}</option>{/each}
			</select>
		</label>
	</div>
	<div class="form-pair">
		<label class="field">
			Date
			<input bind:value={date} type="date" required />
		</label>
		<label class="field">
			Note <span>(optional)</span>
			<input bind:value={note} maxlength="500" placeholder="Anything useful" />
		</label>
	</div>
	{#if error}<p class="error" role="alert">{error}</p>{/if}
	<div class="form-actions">
		{#if onCancel}<button class="quiet" type="button" onclick={onCancel}>Cancel</button>{/if}
		<button class="primary" disabled={submitting}>{submitting ? 'Saving…' : submitLabel}</button>
	</div>
</form>
