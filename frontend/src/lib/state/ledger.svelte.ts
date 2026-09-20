import { del, get, post, put } from '../api/client';
import {
	categoriesSchema,
	dashboardSchema,
	expensePageSchema,
	expenseSchema,
	type Category,
	type Dashboard,
	type Expense,
	type ExpenseDraft,
	type ExpenseHistoryFilters,
	type ExpensePage
} from '../api/types';

/**
 * The seam for the personal expense ledger: categories, the dashboard summary,
 * paginated history, and expense CRUD. Callers read `ledger.categories`
 * reactively and call the methods below instead of hitting `$lib/api/client`
 * directly.
 */
class LedgerStore {
	categories = $state.raw<Category[]>([]);

	async loadCategories(): Promise<void> {
		this.categories = await get('/api/categories', categoriesSchema);
	}

	dashboard(): Promise<Dashboard> {
		return get('/api/expenses/dashboard', dashboardSchema);
	}

	/** Fetches one page of history matching `filters`, starting after `cursor`. */
	history(filters: ExpenseHistoryFilters, cursor: string | null): Promise<ExpensePage> {
		const parameters = new URLSearchParams();
		if (filters.query) parameters.set('query', filters.query);
		if (filters.categoryId !== null) parameters.set('categoryId', String(filters.categoryId));
		if (filters.currency) parameters.set('currency', filters.currency);
		if (filters.from) parameters.set('from', filters.from);
		if (filters.to) parameters.set('to', filters.to);
		if (cursor) parameters.set('cursor', cursor);
		const queryString = parameters.toString();
		return get(queryString ? `/api/expenses?${queryString}` : '/api/expenses', expensePageSchema);
	}

	add(draft: ExpenseDraft): Promise<Expense> {
		return post('/api/expenses', draft, expenseSchema);
	}

	update(expenseId: number, draft: ExpenseDraft): Promise<Expense> {
		return put(`/api/expenses/${expenseId}`, draft, expenseSchema);
	}

	remove(expenseId: number): Promise<void> {
		return del(`/api/expenses/${expenseId}`);
	}
}

export const ledger = new LedgerStore();
