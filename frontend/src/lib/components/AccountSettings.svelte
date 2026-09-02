<script lang="ts">
	import type { AccountPasswords } from '$lib/api/types';
	import DefaultCurrencyForm from '$lib/components/DefaultCurrencyForm.svelte';
	import PasswordChangeForm from '$lib/components/PasswordChangeForm.svelte';

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

	let currencyDirty = $state(false);
	let passwordDirty = $state(false);

	$effect(() => {
		onDirty(currencyDirty || passwordDirty);
	});

	function setCurrencyDirty(dirty: boolean) {
		currencyDirty = dirty;
	}

	function setPasswordDirty(dirty: boolean) {
		passwordDirty = dirty;
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
	<DefaultCurrencyForm
		defaultCurrency={defaultCurrency}
		saving={savingCurrency}
		serverError={currencyError}
		onDirty={setCurrencyDirty}
		onSave={onSaveCurrency}
	>
		{#snippet heading()}
			<h2>Default currency</h2>
			<p class="intro">Used for new expense forms.</p>
		{/snippet}
	</DefaultCurrencyForm>
	<PasswordChangeForm
		saving={savingPassword}
		serverError={passwordError}
		onDirty={setPasswordDirty}
		onChange={onChangePassword}
	>
		{#snippet heading()}
			<h2>Password</h2>
		{/snippet}
	</PasswordChangeForm>
</section>
