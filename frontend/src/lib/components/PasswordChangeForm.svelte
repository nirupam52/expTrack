<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { AccountPasswords } from '$lib/api/types';

	const WRONG_CURRENT_PASSWORD_ERROR = 'Current password is incorrect';

	let {
		heading,
		saving,
		serverError,
		onDirty,
		onChange
	}: {
		heading: Snippet;
		saving: boolean;
		serverError: string;
		onDirty: (dirty: boolean) => void;
		onChange: (passwords: AccountPasswords) => Promise<boolean>;
	} = $props();

	let currentPassword = $state('');
	let newPassword = $state('');
	let newPasswordConfirmation = $state('');
	let currentPasswordFieldError = $state('');
	let passwordFieldError = $state('');
	let passwordErrorField = $state<'new' | 'confirmation' | null>(null);
	let currentPasswordErrorDismissed = $state(false);
	let passwordErrorDismissed = $state(false);

	let passwordTyped = $derived(currentPassword !== '' || newPassword !== '' || newPasswordConfirmation !== '');
	let currentPasswordMessage = $derived(
		currentPasswordFieldError ||
			(serverError === WRONG_CURRENT_PASSWORD_ERROR && !currentPasswordErrorDismissed ? serverError : '')
	);
	let generalPasswordError = $derived(serverError === WRONG_CURRENT_PASSWORD_ERROR ? '' : serverError);
	let generalPasswordMessage = $derived(passwordErrorDismissed ? '' : generalPasswordError);
	let newPasswordMessage = $derived(passwordErrorField === 'new' ? passwordFieldError : generalPasswordMessage);
	let confirmationPasswordMessage = $derived(
		passwordErrorField === 'confirmation' ? passwordFieldError : generalPasswordMessage
	);
	let passwordMessage = $derived(generalPasswordMessage || passwordFieldError);

	$effect(() => {
		onDirty(passwordTyped);
	});

	function submitPassword(event: SubmitEvent) {
		event.preventDefault();
		resetErrors();
		if (!currentPassword) {
			currentPasswordFieldError = 'Enter your current password.';
			return;
		}
		const codePoints = Array.from(newPassword).length;
		if (codePoints < 15 || codePoints > 64) {
			passwordFieldError = 'New password must be 15 to 64 characters.';
			passwordErrorField = 'new';
			return;
		}
		if (newPassword !== newPasswordConfirmation) {
			passwordFieldError = 'New password and confirmation do not match.';
			passwordErrorField = 'confirmation';
			return;
		}
		void onChange({ currentPassword, newPassword, newPasswordConfirmation });
	}

	function resetErrors() {
		currentPasswordFieldError = '';
		passwordFieldError = '';
		passwordErrorField = null;
		currentPasswordErrorDismissed = false;
		passwordErrorDismissed = false;
	}

	function editCurrentPassword() {
		currentPasswordFieldError = '';
		passwordFieldError = '';
		passwordErrorField = null;
		currentPasswordErrorDismissed = true;
		passwordErrorDismissed = true;
	}

	function editPasswordField() {
		passwordFieldError = '';
		passwordErrorField = null;
		passwordErrorDismissed = true;
	}
</script>

<form class="form-stack" novalidate onsubmit={submitPassword}>
	{@render heading()}
	<div class="field">
		<label for="current-password">Current password</label>
		<input
			id="current-password"
			bind:value={currentPassword}
			oninput={editCurrentPassword}
			type="password"
			autocomplete="current-password"
			aria-describedby={currentPasswordMessage ? 'current-password-error' : undefined}
			aria-invalid={currentPasswordMessage ? 'true' : undefined}
			required
		/>
		{#if currentPasswordMessage}<p id="current-password-error" class="error" role="alert">{currentPasswordMessage}</p>{/if}
	</div>
	<div class="field">
		<label for="new-password">New password</label>
		<input
			id="new-password"
			bind:value={newPassword}
			oninput={editPasswordField}
			type="password"
			autocomplete="new-password"
			aria-describedby={newPasswordMessage ? 'password-error' : undefined}
			aria-invalid={newPasswordMessage ? 'true' : undefined}
			required
		/>
	</div>
	<div class="field">
		<label for="new-password-confirmation">Confirm new password</label>
		<input
			id="new-password-confirmation"
			bind:value={newPasswordConfirmation}
			oninput={editPasswordField}
			type="password"
			autocomplete="new-password"
			aria-describedby={confirmationPasswordMessage ? 'password-error' : undefined}
			aria-invalid={confirmationPasswordMessage ? 'true' : undefined}
			required
		/>
	</div>
	{#if passwordMessage}<p id="password-error" class="error" role="alert">{passwordMessage}</p>{/if}
	<div class="form-actions"><button class="primary" disabled={saving}>{saving ? 'Saving…' : 'Change password'}</button></div>
</form>
