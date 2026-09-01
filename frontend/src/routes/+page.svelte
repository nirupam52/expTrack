<script lang="ts">
	import { onMount } from 'svelte';
	import { resolve } from '$app/paths';
	import { createLedgerPageController } from '$lib/ledger-page.svelte';
	import AuthForm from '$lib/components/AuthForm.svelte';
	import AccountPage from '$lib/components/AccountPage.svelte';
	import ExpenseWorkspace from '$lib/components/ExpenseWorkspace.svelte';
	import ProfileMenu from '$lib/components/ProfileMenu.svelte';

	const ledger = createLedgerPageController();

	onMount(() => {
		void ledger.loadSession();
	});
</script>

<svelte:window onbeforeunload={ledger.warnBeforeUnload} />
<svelte:head><title>ExpTrack</title><meta name="description" content="A private place to record everyday spending." /></svelte:head>

<main class="app">
	<header>
		<a class="brand" href={resolve('/')} onclick={(event) => { if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return; event.preventDefault(); ledger.viewDashboard(); }}>ExpTrack</a>
		{#if ledger.model.session}
			<nav class="bottom-nav" aria-label="Main navigation"><button class={{ active: ledger.model.view === 'dashboard' }} aria-current={ledger.model.view === 'dashboard' ? 'page' : undefined} onclick={ledger.viewDashboard}>Dashboard</button><button class={{ active: ledger.model.view === 'add' }} aria-current={ledger.model.view === 'add' ? 'page' : undefined} onclick={ledger.startAddExpense}>Add expense</button><button class={{ active: ledger.model.view === 'history' }} aria-current={ledger.model.view === 'history' ? 'page' : undefined} onclick={() => ledger.viewHistory()}>History</button></nav>
			<ProfileMenu signOutError={ledger.model.signOutError} onAccountSettings={ledger.openAccountSettings} onSignOut={() => void ledger.signOut()} />
		{/if}
	</header>

	{#if ledger.model.loading}
		<p class="status">Loading your ledger…</p>
	{:else if ledger.model.loadError}
		<section class="auth" aria-labelledby="load-error-title">
			<h1 id="load-error-title">Your ledger is unavailable.</h1>
			<p class="error" role="alert">{ledger.model.loadError}</p>
			<button class="primary" onclick={ledger.loadSession}>Retry</button>
		</section>
	{:else if !ledger.model.session}
		<AuthForm submitting={ledger.model.submitting} error={ledger.model.error} notice={ledger.model.authNotice} onSubmit={ledger.authenticate} onModeChange={ledger.clearAuthError} />
	{:else if ledger.model.view === 'account'}
		<AccountPage
			session={ledger.model.session}
			savingCurrency={ledger.model.savingCurrency}
			savingPassword={ledger.model.savingPassword}
			currencyError={ledger.model.currencyError}
			passwordError={ledger.model.passwordError}
			onDirty={ledger.setAccountDirty}
			onSaveCurrency={ledger.saveDefaultCurrency}
			onChangePassword={ledger.changePassword}
		/>
	{:else}
		<ExpenseWorkspace
			session={ledger.model.session}
			categories={ledger.model.categories}
			view={ledger.model.view}
			editing={ledger.model.editing}
			submitting={ledger.model.submitting}
			error={ledger.model.error}
			expenseVersion={ledger.model.expenseVersion}
			historyFilters={ledger.model.historyFilters}
			historyRemount={ledger.model.historyRemount}
			onStartAddExpense={ledger.startAddExpense}
			onAddExpense={ledger.addExpense}
			onUpdateExpense={ledger.updateExpense}
			onCancelEdit={ledger.cancelEdit}
			onViewHistory={ledger.viewHistory}
			onEdit={ledger.editExpense}
			onDelete={ledger.deleteExpense}
			onFiltersChange={ledger.updateHistoryFilters}
			onDirty={ledger.markExpenseDirty}
		/>
		{#if ledger.model.toast}<p class:toast-error={ledger.model.toast.kind === 'error'} class="toast" role={ledger.model.toast.kind === 'error' ? 'alert' : 'status'}>{ledger.model.toast.message}</p>{/if}
	{/if}
</main>
