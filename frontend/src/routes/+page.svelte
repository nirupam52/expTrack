<script lang="ts">
	import { onMount } from 'svelte';
	import { resolve } from '$app/paths';
	import { del, get, HttpError, post, put } from '$lib/api/client';
	import {
		categoriesSchema,
		expenseSchema,
		sessionSchema,
		type AuthSubmission,
		type Category,
		type Expense,
		type ExpenseDraft,
		type ExpenseHistoryFilters,
		type Session
	} from '$lib/api/types';
	import AuthForm from '$lib/components/AuthForm.svelte';
	import ExpenseForm from '$lib/components/ExpenseForm.svelte';
	import ExpenseHistory from '$lib/components/ExpenseHistory.svelte';
	import Dashboard from '$lib/components/Dashboard.svelte';
	import { today } from '$lib/utils/date';
	import { amountForInput } from '$lib/utils/format-currency';

	type Toast = {
		id: number;
		kind: 'success' | 'error';
		message: string;
	};
	type View = 'dashboard' | 'add' | 'history';
	const emptyHistoryFilters: ExpenseHistoryFilters = { query: '', categoryId: null, currency: '', from: '', to: '' };

	let session = $state<Session | null>(null);
	let categories = $state.raw<Category[]>([]);
	let loading = $state(true);
	let submitting = $state(false);
	let loadError = $state('');
	let signOutError = $state('');
	let error = $state('');
	let editing = $state<Expense | null>(null);
	let expenseVersion = $state(0);
	let view = $state<View>('dashboard');
	let formDirty = $state(false);
	let historyFilters = $state<ExpenseHistoryFilters>({ ...emptyHistoryFilters });
	let toast = $state<Toast | null>(null);
	let toastSequence = 0;

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
			categories = await get('/api/categories', categoriesSchema);
		} catch {
			loadError = 'Unable to load your ledger. Please try again.';
		}
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
			await post('/api/expenses', draft, expenseSchema);
			expenseVersion += 1;
			formDirty = false;
			return true;
		} catch (cause) {
			error = expenseError(cause, 'Could not save the expense. Please try again.');
			return false;
		} finally {
			submitting = false;
		}
	}

	async function updateExpense(draft: ExpenseDraft) {
		if (!editing) return false;
		error = '';
		submitting = true;
		try {
			await put(`/api/expenses/${editing.id}`, draft, expenseSchema);
			expenseVersion += 1;
			formDirty = false;
			return true;
		} catch (cause) {
			error = expenseError(cause, 'Could not update the expense. Please try again.');
			return false;
		} finally {
			submitting = false;
		}
	}

	async function deleteExpense(expense: Expense) {
		try {
			await del(`/api/expenses/${expense.id}`);
			if (editing?.id === expense.id) {
				editing = null;
				error = '';
			}
			expenseVersion += 1;
			showToast('success', 'Expense deleted.');
			return true;
		} catch {
			showToast('error', 'Could not delete the expense. Please try again.');
			return false;
		}
	}

	function expenseError(cause: unknown, fallback: string) {
		return cause instanceof HttpError && cause.status === 400
			? `${cause.detail ? `${cause.detail}. ` : ''}Enter a title, positive amount, category, and date.`
			: fallback;
	}

	function showToast(kind: Toast['kind'], message: string) {
		const id = ++toastSequence;
		toast = { id, kind, message };
		window.setTimeout(() => {
			if (toast?.id === id) toast = null;
		}, 5000);
	}

	async function signOut() {
		if (!canLeaveForm()) return;
		signOutError = '';
		try {
			await post('/api/auth/logout', null);
			session = null;
			editing = null;
			view = 'dashboard';
		} catch {
			signOutError = 'Could not sign out. Please try again.';
		}
	}

	function viewHistory(filters: Partial<ExpenseHistoryFilters> = {}) {
		if (!canLeaveForm()) return;
		historyFilters = { ...emptyHistoryFilters, ...filters };
		editing = null;
		formDirty = false;
		view = 'history';
	}

	function startAddExpense() {
		if (view === 'add') return;
		editing = null;
		error = '';
		formDirty = false;
		view = 'add';
	}

	function viewDashboard() {
		if (!canLeaveForm()) return;
		editing = null;
		formDirty = false;
		view = 'dashboard';
	}

	function canLeaveForm() {
		return view !== 'add' || !formDirty || window.confirm('Discard the changes to this expense?');
	}

	function monthEnd(month: string) {
		const [year, monthNumber] = month.split('-').map(Number);
		return `${month}-${new Date(Date.UTC(year, monthNumber, 0)).getUTCDate().toString().padStart(2, '0')}`;
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
		{#if view === 'dashboard'}
			<Dashboard {categories} defaultCurrency={session.defaultCurrency} onAddExpense={startAddExpense} onViewHistory={() => viewHistory()} onViewCategory={(categoryId, month, currency) => viewHistory({ categoryId, currency, from: `${month}-01`, to: monthEnd(month) })} />
		{:else if view === 'add'}
			<section class="ledger" aria-labelledby="add-expense-title">
				<div class="heading"><div><p class="eyebrow">{editing ? 'Correction' : 'Your ledger'}</p><h1 id="add-expense-title">{editing ? 'Edit expense' : 'Add an expense'}</h1></div><p>{editing?.currency ?? session.defaultCurrency}</p></div>
				{#if editing}{#key editing.id}<ExpenseForm {categories} initial={{ title: editing.title, amount: amountForInput(editing), categoryId: editing.categoryId, date: editing.date, note: editing.note ?? '' }} {submitting} {error} submitLabel="Save changes" onCancel={() => { editing = null; error = ''; formDirty = false; view = 'history'; }} onDirty={() => formDirty = true} onSubmit={updateExpense} />{/key}{:else}<ExpenseForm {categories} initial={{ title: '', amount: '', categoryId: categories[0]?.id ?? null, date: today(), note: '' }} {submitting} {error} onDirty={() => formDirty = true} onSubmit={addExpense} />{/if}
			</section>
		{:else}
			{#key `${historyFilters.query}-${historyFilters.categoryId}-${historyFilters.currency}-${historyFilters.from}-${historyFilters.to}`}<ExpenseHistory {categories} initialFilters={historyFilters} reloadVersion={expenseVersion} onEdit={(expense) => { editing = expense; error = ''; view = 'add'; }} onDelete={deleteExpense} />{/key}
		{/if}
		<nav class="bottom-nav" aria-label="Main navigation"><button class={{ active: view === 'dashboard' }} aria-current={view === 'dashboard' ? 'page' : undefined} onclick={viewDashboard}>Dashboard</button><button class={{ active: view === 'add' }} aria-current={view === 'add' ? 'page' : undefined} onclick={startAddExpense}>Add expense</button><button class={{ active: view === 'history' }} aria-current={view === 'history' ? 'page' : undefined} onclick={() => viewHistory()}>History</button></nav>
		{#if toast}<p class:toast-error={toast.kind === 'error'} class="toast" role={toast.kind === 'error' ? 'alert' : 'status'}>{toast.message}</p>{/if}
	{/if}
</main>
