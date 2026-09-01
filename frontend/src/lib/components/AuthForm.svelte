<script lang="ts">
	import type { AuthSubmission } from '$lib/api/types';

	let { submitting, error, notice = '', onSubmit, onModeChange }: { submitting: boolean; error: string; notice?: string; onSubmit: (submission: AuthSubmission) => void; onModeChange: () => void } = $props();

	let mode = $state<'sign-in' | 'register'>('sign-in');
	let email = $state('');
	let password = $state('');
	let defaultCurrency = $state('USD');
	let passwordError = $state('');

	function submit(event: SubmitEvent) {
		event.preventDefault();
		if (mode === 'register') {
			const passwordLength = Array.from(password).length;
			if (passwordLength < 15 || passwordLength > 64) {
				passwordError = 'Password must be between 15 and 64 characters';
				return;
			}
		}
		passwordError = '';
		onSubmit({ mode, email, password, defaultCurrency });
	}

	function clearPasswordError() {
		passwordError = '';
	}

	function toggleMode() {
		mode = mode === 'sign-in' ? 'register' : 'sign-in';
		clearPasswordError();
		onModeChange();
	}
</script>

<section class="auth" aria-labelledby="welcome-title">
	<p class="eyebrow">Personal expense tracker</p>
	<h1 id="welcome-title">Know where it went.</h1>
	<p class="intro">Record an expense in a few seconds. Your data stays private to your account.</p>
	{#if notice}<p class="notice" role="status">{notice}</p>{/if}
	<form class="form-stack" onsubmit={submit}>
		<div class="field">
			<label for="auth-email">Email</label>
			<input id="auth-email" bind:value={email} type="email" autocomplete="email" required />
		</div>
		<div class="field">
			<label for="auth-password">Password</label>
			<input
				id="auth-password"
				bind:value={password}
				oninput={clearPasswordError}
				type="password"
				autocomplete={mode === 'sign-in' ? 'current-password' : 'new-password'}
				aria-describedby={passwordError ? 'password-error' : undefined}
				aria-invalid={passwordError ? 'true' : undefined}
				required
			/>
			{#if passwordError}<p id="password-error" class="error" role="alert">{passwordError}</p>{/if}
		</div>
		{#if mode === 'register'}<label class="field">Default currency <input bind:value={defaultCurrency} maxlength="3" autocapitalize="characters" required /></label>{/if}
		<button class="primary" disabled={submitting}>{submitting ? 'Working…' : mode === 'sign-in' ? 'Sign in' : 'Create account'}</button>
	</form>
	<button class="switch" onclick={toggleMode}>
		{mode === 'sign-in' ? 'Create an account' : 'Already have an account? Sign in'}
	</button>
</section>
