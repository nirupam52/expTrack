<script lang="ts">
	import { onMount } from 'svelte';

	type Category = { name: string };
	type Session = { email: string; defaultCurrency: string };
	type Expense = { id: number; title: string; amountMinor: number; category: string; date: string; currency: string; note: string | null };

	let session = $state<Session | null>(null);
	let categories = $state<Category[]>([]);
	let expenses = $state<Expense[]>([]);
	let mode = $state<'sign-in' | 'register'>('sign-in');
	let loading = $state(true);
	let submitting = $state(false);
	let error = $state('');
	let email = $state('');
	let password = $state('');
	let defaultCurrency = $state('USD');
	let title = $state('');
	let amount = $state('');
	let category = $state('');
	let date = $state(today());
	let note = $state('');

	onMount(loadSession);

	function today() {
		const now = new Date();
		return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
	}

	async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
		const response = await fetch(path, { credentials: 'same-origin', ...init });
		if (!response.ok) throw new Error();
		return response.json() as Promise<T>;
	}

	async function csrfToken() {
		return (await fetchJson<{ token: string }>('/api/auth/csrf')).token;
	}

	async function loadSession() {
		try {
			session = await fetchJson<Session>('/api/auth/session');
			await loadExpenses();
		} catch {
			session = null;
		} finally {
			loading = false;
		}
	}

	async function loadExpenses() {
		[expenses, categories] = await Promise.all([fetchJson<Expense[]>('/api/expenses'), fetchJson<Category[]>('/api/categories')]);
		category ||= categories[0]?.name ?? '';
	}

	async function authenticate() {
		error = '';
		submitting = true;
		try {
			if (mode === 'register') {
				await send('/api/auth/register', { email, password, defaultCurrency: defaultCurrency.trim().toUpperCase() });
			}
			await send('/api/auth/login', new URLSearchParams({ username: email, password }), 'application/x-www-form-urlencoded');
			await loadSession();
		} catch {
			error = mode === 'register' ? 'Check the account details and try again.' : 'Email or password is incorrect.';
		} finally {
			submitting = false;
		}
	}

	async function addExpense() {
		error = '';
		submitting = true;
		try {
			const created = await send<Expense>('/api/expenses', { title, amount, category, date, note });
			expenses = [created, ...expenses].slice(0, 10);
			title = '';
			amount = '';
			note = '';
			date = today();
		} catch {
			error = 'Check the expense details and try again.';
		} finally {
			submitting = false;
		}
	}

	async function send<T = void>(path: string, body: unknown, contentType = 'application/json'): Promise<T> {
		const response = await fetch(path, {
			method: 'POST',
			credentials: 'same-origin',
			headers: { 'Content-Type': contentType, 'X-CSRF-TOKEN': await csrfToken() },
			body: contentType === 'application/json' ? JSON.stringify(body) : body as BodyInit
		});
		if (!response.ok) throw new Error();
		return response.headers.get('content-type')?.includes('application/json') ? response.json() as Promise<T> : undefined as T;
	}

	async function signOut() {
		await send('/api/auth/logout', null);
		session = null;
		expenses = [];
	}

	function formattedAmount(expense: Expense) {
		const fractionDigits = new Intl.NumberFormat(undefined, { style: 'currency', currency: expense.currency }).resolvedOptions().maximumFractionDigits ?? 2;
		return new Intl.NumberFormat(undefined, { style: 'currency', currency: expense.currency }).format(expense.amountMinor / 10 ** fractionDigits);
	}
</script>

<svelte:head><title>ExpTrack</title><meta name="description" content="A private place to record everyday spending." /></svelte:head>

<main>
	<header>
		<a class="brand" href="/">ExpTrack</a>
		{#if session}<button class="quiet" onclick={signOut}>Sign out</button>{/if}
	</header>

	{#if loading}
		<p class="status">Loading your ledger…</p>
	{:else if !session}
		<section class="auth" aria-labelledby="welcome-title">
			<p class="eyebrow">Personal expense tracker</p>
			<h1 id="welcome-title">Know where it went.</h1>
			<p class="intro">Record an expense in a few seconds. Your data stays private to your account.</p>
			<form onsubmit={(event) => { event.preventDefault(); authenticate(); }}>
				<label>Email <input bind:value={email} type="email" autocomplete="email" required /></label>
				<label>Password <input bind:value={password} type="password" autocomplete={mode === 'sign-in' ? 'current-password' : 'new-password'} minlength="12" required /></label>
				{#if mode === 'register'}<label>Default currency <input bind:value={defaultCurrency} maxlength="3" autocapitalize="characters" required /></label>{/if}
				{#if error}<p class="error" role="alert">{error}</p>{/if}
				<button class="primary" disabled={submitting}>{submitting ? 'Working…' : mode === 'sign-in' ? 'Sign in' : 'Create account'}</button>
			</form>
			<button class="switch" onclick={() => { mode = mode === 'sign-in' ? 'register' : 'sign-in'; error = ''; }}>
				{mode === 'sign-in' ? 'Create an account' : 'Already have an account? Sign in'}
			</button>
		</section>
	{:else}
		<section class="ledger" aria-labelledby="add-expense-title">
			<div class="heading"><div><p class="eyebrow">Your ledger</p><h1 id="add-expense-title">Add an expense</h1></div><p>{session.defaultCurrency}</p></div>
			<form class="expense-form" onsubmit={(event) => { event.preventDefault(); addExpense(); }}>
				<label>What was it?<input bind:value={title} maxlength="120" placeholder="Coffee" autocomplete="off" required /></label>
				<div class="pair"><label>Amount<input bind:value={amount} inputmode="decimal" placeholder="12.34" required /></label><label>Category<select bind:value={category} required>{#each categories as item}<option value={item.name}>{item.name}</option>{/each}</select></label></div>
				<div class="pair"><label>Date<input bind:value={date} type="date" required /></label><label>Note <span>(optional)</span><input bind:value={note} maxlength="500" placeholder="Anything useful" /></label></div>
				{#if error}<p class="error" role="alert">{error}</p>{/if}
				<button class="primary" disabled={submitting}>{submitting ? 'Saving…' : 'Save expense'}</button>
			</form>
		</section>
		<section class="recent" aria-labelledby="recent-title">
			<div class="heading"><div><p class="eyebrow">Latest entries</p><h2 id="recent-title">Recently added</h2></div><p>{expenses.length}</p></div>
			{#if expenses.length === 0}<p class="empty">Your first saved expense will appear here.</p>
			{:else}<ul>{#each expenses as expense}<li><div><strong>{expense.title}</strong><span>{expense.category} · {expense.date}{#if expense.note} · {expense.note}{/if}</span></div><b>{formattedAmount(expense)}</b></li>{/each}</ul>{/if}
		</section>
	{/if}
</main>

<style>
	:global(*) { box-sizing: border-box; }
	:global(body) { margin: 0; background: #edf3f2; color: #183034; font-family: Inter, ui-sans-serif, system-ui, sans-serif; }
	:global(button), :global(input), :global(select) { font: inherit; }
	main { width: min(100% - 2rem, 42rem); margin: auto; padding: 1.25rem 0 3rem; }
	header, .heading, li { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
	header { border-bottom: 1px solid #c7d7d5; padding-bottom: 1rem; }
	.brand { color: inherit; font-size: 1.25rem; font-weight: 800; letter-spacing: -.05em; text-decoration: none; }
	.auth, .ledger, .recent { margin-top: 2rem; }
	.auth { max-width: 30rem; }
	.eyebrow { color: #477176; font-size: .73rem; font-weight: 800; letter-spacing: .12em; margin: 0 0 .45rem; text-transform: uppercase; }
	h1, h2, p { margin-top: 0; }
	h1 { font-size: clamp(2.25rem, 10vw, 4rem); letter-spacing: -.06em; line-height: .94; margin-bottom: 1rem; }
	h2 { font-size: 1.3rem; letter-spacing: -.04em; margin-bottom: 0; }
	.intro, .empty, .heading > p { color: #537176; line-height: 1.5; }
	form { display: grid; gap: 1rem; margin-top: 1.75rem; }
	label { display: grid; color: #35565a; font-size: .88rem; font-weight: 700; gap: .4rem; }
	label span { font-weight: 400; }
	input, select { appearance: none; background: #fff; border: 1px solid #b9cecb; border-radius: .4rem; color: #183034; min-height: 2.9rem; padding: .65rem .75rem; width: 100%; }
	input:focus, select:focus, button:focus-visible { outline: 3px solid #76b8af; outline-offset: 2px; }
	.pair { display: grid; gap: 1rem; grid-template-columns: 1fr; }
	.primary { background: #0f6861; border: 0; border-radius: .4rem; color: white; cursor: pointer; font-weight: 800; min-height: 3rem; padding: .7rem 1rem; }
	.primary:disabled { cursor: wait; opacity: .65; }
	.quiet, .switch { background: none; border: 0; color: #0f6861; cursor: pointer; font-weight: 700; padding: .3rem 0; text-align: left; }
	.switch { margin-top: 1rem; }
	.error { color: #9a2e25; font-size: .9rem; margin: 0; }
	.recent { border-top: 1px solid #c7d7d5; padding-top: 1.5rem; }
	ul { list-style: none; margin: 1rem 0 0; padding: 0; }
	li { border-bottom: 1px solid #d3e0de; padding: .9rem 0; }
	li div { min-width: 0; }
	li strong, li span { display: block; }
	li span { color: #537176; font-size: .85rem; margin-top: .2rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	li b { font-variant-numeric: tabular-nums; white-space: nowrap; }
	.status { color: #537176; margin-top: 3rem; }
	@media (min-width: 35rem) { .pair { grid-template-columns: 1fr 1fr; } }
</style>
