<script lang="ts">
	import { onMount } from 'svelte';
	import { resolve } from '$app/paths';
	import { createLedgerPageController } from '$lib/ledger-page.svelte';
	import AuthForm from '$lib/components/AuthForm.svelte';
	import AccountSettings from '$lib/components/AccountSettings.svelte';
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
		{#if ledger.session}
			<nav class="bottom-nav" aria-label="Main navigation"><button class={{ active: ledger.view === 'dashboard' }} aria-current={ledger.view === 'dashboard' ? 'page' : undefined} onclick={ledger.viewDashboard}>Dashboard</button><button class={{ active: ledger.view === 'add' }} aria-current={ledger.view === 'add' ? 'page' : undefined} onclick={ledger.startAddExpense}>Add expense</button><button class={{ active: ledger.view === 'history' }} aria-current={ledger.view === 'history' ? 'page' : undefined} onclick={() => ledger.viewHistory()}>History</button></nav>
			<ProfileMenu signOutError={ledger.signOutError} onAccountSettings={ledger.openAccountSettings} onSignOut={() => void ledger.signOut()} />
		{/if}
	</header>

	{#if ledger.loading}
		<p class="status">Loading your ledger…</p>
	{:else if ledger.loadError}
		<section class="auth" aria-labelledby="load-error-title">
			<h1 id="load-error-title">Your ledger is unavailable.</h1>
			<p class="error" role="alert">{ledger.loadError}</p>
			<button class="primary" onclick={ledger.loadSession}>Retry</button>
		</section>
	{:else if !ledger.session}
		<AuthForm submitting={ledger.submitting} error={ledger.error} notice={ledger.authNotice} onSubmit={ledger.authenticate} onModeChange={ledger.clearAuthError} />
	{:else if ledger.view === 'account'}
		<AccountSettings
			email={ledger.session.email}
			createdAt={ledger.session.createdAt}
			defaultCurrency={ledger.session.defaultCurrency}
			savingCurrency={ledger.savingCurrency}
			savingPassword={ledger.savingPassword}
			currencyError={ledger.currencyError}
			passwordError={ledger.passwordError}
			onDirty={ledger.setAccountDirty}
			onSaveCurrency={ledger.saveDefaultCurrency}
			onChangePassword={ledger.changePassword}
		/>
	{:else}
		<ExpenseWorkspace
			session={ledger.session}
			categories={ledger.categories}
			view={ledger.view}
			editing={ledger.editing}
			submitting={ledger.submitting}
			error={ledger.error}
			expenseVersion={ledger.expenseVersion}
			historyFilters={ledger.historyFilters}
			historyRemount={ledger.historyRemount}
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
		{#if ledger.toast}<p class:toast-error={ledger.toast.kind === 'error'} class="toast" role={ledger.toast.kind === 'error' ? 'alert' : 'status'}>{ledger.toast.message}</p>{/if}
	{/if}
</main>
