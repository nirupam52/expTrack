<script lang="ts">
	import type { Category, Expense, ExpenseDraft, ExpenseHistoryFilters, Session } from '$lib/api/types';
	import Dashboard from '$lib/components/Dashboard.svelte';
	import ExpenseForm from '$lib/components/ExpenseForm.svelte';
	import ExpenseHistory from '$lib/components/ExpenseHistory.svelte';
	import { amountForInput } from '$lib/utils/format-currency';
	import { today } from '$lib/utils/date';

	type View = 'dashboard' | 'add' | 'history' | 'account';

	let {
		session,
		categories,
		view,
		editing,
		submitting,
		error,
		expenseVersion,
		historyFilters,
		historyRemount,
		onStartAddExpense,
		onAddExpense,
		onUpdateExpense,
		onCancelEdit,
		onViewHistory,
		onEdit,
		onDelete,
		onFiltersChange,
		onDirty
	}: {
		session: Session;
		categories: Category[];
		view: View;
		editing: Expense | null;
		submitting: boolean;
		error: string;
		expenseVersion: number;
		historyFilters: ExpenseHistoryFilters;
		historyRemount: number;
		onStartAddExpense: () => void;
		onAddExpense: (draft: ExpenseDraft) => Promise<boolean>;
		onUpdateExpense: (draft: ExpenseDraft) => Promise<boolean>;
		onCancelEdit: () => void;
		onViewHistory: (filters?: Partial<ExpenseHistoryFilters>) => void;
		onEdit: (expense: Expense) => void;
		onDelete: (expense: Expense) => Promise<boolean>;
		onFiltersChange: (filters: ExpenseHistoryFilters) => void;
		onDirty: () => void;
	} = $props();

	function monthEnd(month: string) {
		const [year, monthNumber] = month.split('-').map(Number);
		return `${month}-${new Date(Date.UTC(year, monthNumber, 0)).getUTCDate().toString().padStart(2, '0')}`;
	}

	function viewCategory(categoryId: number, month: string, currency: string) {
		onViewHistory({ categoryId, currency, from: `${month}-01`, to: monthEnd(month) });
	}
</script>

{#if view === 'dashboard'}
	<Dashboard
		{categories}
		defaultCurrency={session.defaultCurrency}
		onAddExpense={onStartAddExpense}
		onViewHistory={() => onViewHistory()}
		onViewCategory={viewCategory}
	/>
{:else if view === 'add'}
	<section class="ledger" aria-labelledby="add-expense-title">
		<div class="heading">
			<div><p class="eyebrow">{editing ? 'Correction' : 'Your ledger'}</p><h1 id="add-expense-title">{editing ? 'Edit expense' : 'Add an expense'}</h1></div>
			<p>{editing?.currency ?? session.defaultCurrency}</p>
		</div>
		{#if editing}
			{#key editing.id}
				<ExpenseForm
					{categories}
					initial={{ title: editing.title, amount: amountForInput(editing), categoryId: editing.categoryId, date: editing.date, note: editing.note ?? '' }}
					{submitting}
					{error}
					submitLabel="Save changes"
					onCancel={onCancelEdit}
					onDirty={onDirty}
					onSubmit={onUpdateExpense}
				/>
			{/key}
		{:else}
			<ExpenseForm
				{categories}
				initial={{ title: '', amount: '', categoryId: categories[0]?.id ?? null, date: today(), note: '', currencySnapshot: session.currencySnapshot }}
				{submitting}
				{error}
				onDirty={onDirty}
				onSubmit={onAddExpense}
			/>
		{/if}
	</section>
{:else if view === 'history'}
	{#key historyRemount}
		<ExpenseHistory
			{categories}
			initialFilters={historyFilters}
			reloadVersion={expenseVersion}
			onEdit={onEdit}
			onDelete={onDelete}
			onFiltersChange={onFiltersChange}
		/>
	{/key}
{/if}
