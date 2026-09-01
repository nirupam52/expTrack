<script lang="ts">
	import type { AuthSubmission } from '$lib/api/types';

	let { submitting, error, notice = '', onSubmit, onModeChange }: { submitting: boolean; error: string; notice?: string; onSubmit: (submission: AuthSubmission) => void; onModeChange: () => void } = $props();

	let mode = $state<'sign-in' | 'register'>('sign-in');
	let email = $state('');
	let password = $state('');
	let defaultCurrency = $state('USD');

	function submit(event: SubmitEvent) {
		event.preventDefault();
		onSubmit({ mode, email, password, defaultCurrency });
	}

	function toggleMode() {
		mode = mode === 'sign-in' ? 'register' : 'sign-in';
		onModeChange();
	}
</script>

<section class="auth" aria-labelledby="welcome-title">
	<p class="eyebrow">Personal expense tracker</p>
	<h1 id="welcome-title">Know where it went.</h1>
	<p class="intro">Record an expense in a few seconds. Your data stays private to your account.</p>
	{#if notice}<p class="notice" role="status">{notice}</p>{/if}
	<form class="form-stack" onsubmit={submit}>
		<label class="field">Email <input bind:value={email} type="email" autocomplete="email" required /></label>
		<label class="field">Password <input bind:value={password} type="password" autocomplete={mode === 'sign-in' ? 'current-password' : 'new-password'} minlength="15" required /></label>
		{#if mode === 'register'}<label class="field">Default currency <input bind:value={defaultCurrency} maxlength="3" autocapitalize="characters" required /></label>{/if}
		{#if error}<p class="error" role="alert">{error}</p>{/if}
		<button class="primary" disabled={submitting}>{submitting ? 'Working…' : mode === 'sign-in' ? 'Sign in' : 'Create account'}</button>
	</form>
	<button class="switch" onclick={toggleMode}>
		{mode === 'sign-in' ? 'Create an account' : 'Already have an account? Sign in'}
	</button>
</section>
