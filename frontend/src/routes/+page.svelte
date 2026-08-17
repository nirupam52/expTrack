<script lang="ts">
	import { onMount } from 'svelte';
	import { resolve } from '$app/paths';
	import { get, HttpError, post } from '$lib/api/client';
	import {
		categoriesSchema,
		expenseSchema,
		expensesSchema,
		sessionSchema,
		type AuthSubmission,
		type Category,
		type Expense,
		type ExpenseDraft,
		type Session
	} from '$lib/api/types';
	import AuthForm from '$lib/components/AuthForm.svelte';
	import ExpenseForm from '$lib/components/ExpenseForm.svelte';
	import RecentExpenses from '$lib/components/RecentExpenses.svelte';

	let session = $state<Session | null>(null);
	let categories = $state.raw<Category[]>([]);
	let expenses = $state.raw<Expense[]>([]);
	let loading = $state(true);
	let submitting = $state(false);
	let loadError = $state('');
	let signOutError = $state('');
	let error = $state('');
	let category = $state<number | null>(null);

	onMount(() => {
		void loadSession();
	});

	async function loadSession() {
		loading = true;
		loadError = '';
		try {
			session = await get('/api/auth/session', sessionSchema);
		} catch (cause) {
			handleSessionLoadError(cause);
			return;
		}
		await loadLedger();
		loading = false;
	}

	function handleSessionLoadError(cause: unknown) {
		if (cause instanceof HttpError && cause.status === 401) session = null;
		else loadError = 'Unable to load your ledger. Please try again.';
		loading = false;
	}

	async function loadLedger() {
		try {
			await loadExpenses();
		} catch {
			loadError = 'Unable to load your ledger. Please try again.';
		}
	}

	async function loadExpenses() {
		[expenses, categories] = await Promise.all([
			get('/api/expenses', expensesSchema),
			get('/api/categories', categoriesSchema)
		]);
		category ??= categories[0]?.id ?? null;
	}

	async function authenticate({ mode, email, password, defaultCurrency }: AuthSubmission) {
		error = '';
		submitting = true;
		try {
			if (mode === 'register') {
				await post('/api/auth/register', { email, password, defaultCurrency: defaultCurrency.trim().toUpperCase() });
			}
			await post('/api/auth/login', new URLSearchParams({ username: email, password }), 'application/x-www-form-urlencoded');
			await loadSession();
		} catch {
			error = mode === 'register' ? 'Check the account details and try again.' : 'Email or password is incorrect.';
		} finally {
			submitting = false;
		}
	}

	async function addExpense(draft: ExpenseDraft) {
		error = '';
		submitting = true;
		try {
			const created = await post('/api/expenses', draft, expenseSchema);
			expenses = [created, ...expenses].slice(0, 10);
			return true;
		} catch (cause) {
			error = cause instanceof HttpError && cause.status === 400
				? `${cause.detail ? `${cause.detail}. ` : ''}Enter a title, positive amount, category, and date.`
				: 'Could not save the expense. Please try again.';
			return false;
		} finally {
			submitting = false;
		}
	}

	async function signOut() {
		signOutError = '';
		try {
			await post('/api/auth/logout', null);
			session = null;
			expenses = [];
		} catch {
			signOutError = 'Could not sign out. Please try again.';
		}
	}

</script>

<svelte:head><title>ExpTrack</title><meta name="description" content="A private place to record everyday spending." /></svelte:head>

<main class="app">
	<header>
		<a class="brand" href={resolve('/')}>ExpTrack</a>
		{#if session}
			<button class="quiet" onclick={signOut}>Sign out</button>
			{#if signOutError}<p class="error" role="alert">{signOutError}</p>{/if}
		{/if}
	</header>

	{#if loading}
		<p class="status">Loading your ledger…</p>
	{:else if loadError}
		<section class="auth" aria-labelledby="load-error-title">
			<h1 id="load-error-title">Your ledger is unavailable.</h1>
			<p class="error" role="alert">{loadError}</p>
			<button class="primary" onclick={loadSession}>Retry</button>
		</section>
	{:else if !session}
		<AuthForm {submitting} {error} onSubmit={authenticate} onModeChange={() => error = ''} />
	{:else}
		<section class="ledger" aria-labelledby="add-expense-title">
			<div class="heading"><div><p class="eyebrow">Your ledger</p><h1 id="add-expense-title">Add an expense</h1></div><p>{session.defaultCurrency}</p></div>
			<ExpenseForm {categories} bind:category {submitting} {error} onSubmit={addExpense} />
		</section>
		<RecentExpenses {expenses} {categories} />
	{/if}
</main>
