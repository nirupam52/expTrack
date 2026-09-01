<script lang="ts">
	import type { AccountPasswords } from '$lib/api/types';

	let {
		email,
		createdAt,
		defaultCurrency,
		savingCurrency,
		savingPassword,
		currencyError,
		passwordError,
		onDirty,
		onSaveCurrency,
		onChangePassword
	}: {
		email: string;
		createdAt: string | null;
		defaultCurrency: string;
		savingCurrency: boolean;
		savingPassword: boolean;
		currencyError: string;
		passwordError: string;
		onDirty: (dirty: boolean) => void;
		onSaveCurrency: (currency: string) => Promise<boolean>;
		onChangePassword: (passwords: AccountPasswords) => Promise<boolean>;
	} = $props();

	let currencyInput = $state('');
	let currencyInitialized = false;
	let currentPassword = $state('');
	let newPassword = $state('');
	let newPasswordConfirmation = $state('');
	let currencyStatus = $state('');
	let currencyFieldError = $state('');
	let passwordFieldError = $state('');

	$effect.pre(() => {
		if (currencyInitialized) return;
		currencyInput = defaultCurrency;
		currencyInitialized = true;
	});

	let currencyChanged = $derived(currencyInput.trim().toUpperCase() !== defaultCurrency);
	let passwordTyped = $derived(currentPassword !== '' || newPassword !== '' || newPasswordConfirmation !== '');
	let currencyMessage = $derived(currencyError || currencyFieldError);
	let passwordMessage = $derived(passwordError || passwordFieldError);

	$effect(() => {
		onDirty(currencyChanged || passwordTyped);
	});

	function editCurrency() {
		currencyStatus = '';
		currencyFieldError = '';
	}

	function submitCurrency(event: SubmitEvent) {
		event.preventDefault();
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
		const saved = await onSaveCurrency(currency);
		if (saved) currencyStatus = 'Default currency saved.';
	}

	function submitPassword(event: SubmitEvent) {
		event.preventDefault();
		passwordFieldError = '';
		if (!currentPassword) {
			passwordFieldError = 'Enter your current password.';
			return;
		}
		const codePoints = Array.from(newPassword).length;
		if (codePoints < 15 || codePoints > 64) {
			passwordFieldError = 'New password must be 15 to 64 characters.';
			return;
		}
		if (newPassword !== newPasswordConfirmation) {
			passwordFieldError = 'New password and confirmation do not match.';
			return;
		}
		void onChangePassword({ currentPassword, newPassword, newPasswordConfirmation });
	}

	function createdLabel() {
		if (!createdAt) return 'Not available';
		return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
			.format(new Date(createdAt));
	}
</script>

<section class="account" aria-labelledby="account-title">
	<p class="eyebrow">Your account</p>
	<h1 id="account-title">Account settings</h1>
	<dl class="account-facts">
		<div><dt>Email</dt><dd>{email}</dd></div>
		<div><dt>Created</dt><dd>{createdLabel()}</dd></div>
	</dl>
	<form class="form-stack" novalidate onsubmit={submitCurrency}>
		<h2>Default currency</h2>
		<p class="intro">Used for new expenses when another currency is not selected.</p>
		<label class="field">
			Currency
			<input bind:value={currencyInput} oninput={editCurrency} autocapitalize="characters" autocomplete="off" required />
		</label>
		{#if currencyMessage}<p class="error" role="alert">{currencyMessage}</p>{/if}
		{#if currencyStatus}<p class="status-note" role="status">{currencyStatus}</p>{/if}
		<div class="form-actions"><button class="primary" disabled={savingCurrency}>{savingCurrency ? 'Saving…' : 'Save default currency'}</button></div>
	</form>
	<form class="form-stack" novalidate onsubmit={submitPassword}>
		<h2>Password</h2>
		<label class="field">
			Current password
			<input bind:value={currentPassword} type="password" autocomplete="current-password" required />
		</label>
		<label class="field">
			New password
			<input bind:value={newPassword} type="password" autocomplete="new-password" required />
		</label>
		<label class="field">
			Confirm new password
			<input bind:value={newPasswordConfirmation} type="password" autocomplete="new-password" required />
		</label>
		{#if passwordMessage}<p class="error" role="alert">{passwordMessage}</p>{/if}
		<div class="form-actions"><button class="primary" disabled={savingPassword}>{savingPassword ? 'Saving…' : 'Change password'}</button></div>
	</form>
</section>
