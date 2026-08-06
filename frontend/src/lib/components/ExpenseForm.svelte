<script lang="ts">
	import type { Category, ExpenseDraft } from '$lib/api/types';
	import { today } from '$lib/utils/date';

	let { categories, category = $bindable(), submitting, error, onSubmit }: {
		categories: Category[];
		category: number | null;
		submitting: boolean;
		error: string;
		onSubmit: (draft: ExpenseDraft) => Promise<boolean>;
	} = $props();

	let title = $state('');
	let amount = $state('');
	let date = $state(today());
	let note = $state('');

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		if (await onSubmit({ title, amount, categoryId: category, date, note })) {
			title = '';
			amount = '';
			note = '';
			date = today();
		}
	}
</script>

<form class="form-stack" onsubmit={submit}>
	<label class="field">What was it?<input bind:value={title} maxlength="120" placeholder="Coffee" autocomplete="off" required /></label>
	<div class="form-pair"><label class="field">Amount<input bind:value={amount} inputmode="decimal" placeholder="12.34" required /></label><label class="field">Category<select bind:value={category} required>{#each categories as item (item.id)}<option value={item.id}>{item.name}</option>{/each}</select></label></div>
	<div class="form-pair"><label class="field">Date<input bind:value={date} type="date" required /></label><label class="field">Note <span>(optional)</span><input bind:value={note} maxlength="500" placeholder="Anything useful" /></label></div>
	{#if error}<p class="error" role="alert">{error}</p>{/if}
	<button class="primary" disabled={submitting}>{submitting ? 'Saving…' : 'Save expense'}</button>
</form>
