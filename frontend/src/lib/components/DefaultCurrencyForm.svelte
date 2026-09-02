<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		heading,
		defaultCurrency,
		saving,
		serverError,
		onDirty,
		onSave
	}: {
		heading: Snippet;
		defaultCurrency: string;
		saving: boolean;
		serverError: string;
		onDirty: (dirty: boolean) => void;
		onSave: (currency: string) => Promise<boolean>;
	} = $props();

	let currencyInput = $state('');
	let currencyInitialized = false;
	let currencyStatus = $state('');
	let currencyFieldError = $state('');
	let currencyErrorDismissed = $state(false);
	let currencySaveRequest = 0;

	$effect.pre(() => {
		if (currencyInitialized) return;
		currencyInput = defaultCurrency;
		currencyInitialized = true;
	});

	let currencyChanged = $derived(currencyInput.trim().toUpperCase() !== defaultCurrency);
	let currencyMessage = $derived(currencyErrorDismissed ? currencyFieldError : serverError || currencyFieldError);

	$effect(() => {
		onDirty(currencyChanged);
	});

	function editCurrency() {
		currencyStatus = '';
		currencyFieldError = '';
		currencyErrorDismissed = true;
	}

	function submitCurrency(event: SubmitEvent) {
		event.preventDefault();
		currencyErrorDismissed = false;
		const normalized = currencyInput.trim().toUpperCase();
		if (!/^[A-Z]{3}$/.test(normalized)) {
			currencyStatus = '';
			currencyFieldError = 'Enter a 3-letter currency code, like USD.';
			return;
		}
		currencyFieldError = '';
		void saveCurrency(normalized);
	}

	async function saveCurrency(currency: string) {
		const request = ++currencySaveRequest;
		const saved = await onSave(currency);
		if (request === currencySaveRequest && saved) {
			currencyInput = currency;
			currencyStatus = 'Default currency saved.';
		}
	}
</script>

<form class="form-stack" novalidate onsubmit={submitCurrency}>
	{@render heading()}
	<label class="field" for="default-currency">
		Currency
		<input
			id="default-currency"
			bind:value={currencyInput}
			oninput={editCurrency}
			autocapitalize="characters"
			autocomplete="off"
			aria-describedby={currencyMessage ? 'currency-error' : undefined}
			aria-invalid={currencyMessage ? 'true' : undefined}
			required
		/>
	</label>
	{#if currencyMessage}<p id="currency-error" class="error" role="alert">{currencyMessage}</p>{/if}
	{#if currencyStatus}<p class="status-note" role="status">{currencyStatus}</p>{/if}
	<div class="form-actions"><button class="primary" disabled={saving}>{saving ? 'Saving…' : 'Save default currency'}</button></div>
</form>
