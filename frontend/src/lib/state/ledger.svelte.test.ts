import { afterEach, beforeEach, expect, test, vi } from 'vitest';

import { errorResponse, jsonResponse, stubFetch } from '../api/fetch-stub.ts';
import { ledger } from './ledger.svelte.ts';
import type { Category, Dashboard, Expense, ExpenseDraft, ExpenseHistoryFilters, ExpensePage } from '../api/types.ts';

const sampleExpense: Expense = {
	id: 42,
	title: 'Coffee',
	amountMinor: '450',
	categoryId: 3,
	date: '2024-01-05',
	currency: 'USD',
	note: null
};

const sampleCategories: Category[] = [
	{ id: 1, name: 'Groceries' },
	{ id: 2, name: 'Transport' }
];

const sampleDashboard: Dashboard = {
	month: '2024-01',
	currencies: [{ currency: 'USD', totalMinor: '450', categories: [{ categoryId: 3, amountMinor: '450' }] }],
	recentExpenses: [sampleExpense]
};

beforeEach(() => {
	ledger.categories = [];
});

afterEach(() => {
	vi.unstubAllGlobals();
});

test('history() with every filter set and a cursor builds the full query string', async () => {
	const page: ExpensePage = { items: [sampleExpense], nextCursor: null };
	const calls = stubFetch((url) => (url.startsWith('/api/expenses?') ? jsonResponse(page) : errorResponse(404)));
	const filters: ExpenseHistoryFilters = { query: 'coffee', categoryId: 3, currency: 'USD', from: '2024-01-01', to: '2024-01-31' };

	const result = await ledger.history(filters, 'abc123');

	expect(result).toEqual(page);
	const call = calls.find((entry) => entry.method === 'GET' && entry.url.startsWith('/api/expenses?'));
	expect(call).toBeDefined();
	const params = new URL(call!.url, 'http://localhost').searchParams;
	expect(params.get('query')).toBe('coffee');
	expect(params.get('categoryId')).toBe('3');
	expect(params.get('currency')).toBe('USD');
	expect(params.get('from')).toBe('2024-01-01');
	expect(params.get('to')).toBe('2024-01-31');
	expect(params.get('cursor')).toBe('abc123');
});

test('history() with every filter empty and no cursor requests the bare path', async () => {
	const page: ExpensePage = { items: [], nextCursor: null };
	const calls = stubFetch((url) => (url === '/api/expenses' ? jsonResponse(page) : errorResponse(404)));
	const filters: ExpenseHistoryFilters = { query: '', categoryId: null, currency: '', from: '', to: '' };

	const result = await ledger.history(filters, null);

	expect(result).toEqual(page);
	const call = calls.find((entry) => entry.method === 'GET');
	expect(call?.url).toBe('/api/expenses');
});

test('dashboard() fetches the current summary', async () => {
	stubFetch((url) => (url === '/api/expenses/dashboard' ? jsonResponse(sampleDashboard) : errorResponse(404)));

	const result = await ledger.dashboard();

	expect(result).toEqual(sampleDashboard);
});

test('add() posts the draft and returns the parsed expense', async () => {
	const draft: ExpenseDraft = { title: 'Coffee', amount: '4.50', categoryId: 3, date: '2024-01-05', note: '', currency: 'USD' };
	const calls = stubFetch((url) => (url === '/api/expenses' ? jsonResponse(sampleExpense) : errorResponse(404)));

	const result = await ledger.add(draft);

	expect(result).toEqual(sampleExpense);
	const call = calls.find((entry) => entry.method === 'POST' && entry.url === '/api/expenses');
	expect(call?.body).toEqual(draft);
});

test('update() issues a PUT to the expense path', async () => {
	const draft: ExpenseDraft = { title: 'Coffee', amount: '4.50', categoryId: 3, date: '2024-01-05', note: '', currency: 'USD' };
	const calls = stubFetch((url) => (url === '/api/expenses/42' ? jsonResponse(sampleExpense) : errorResponse(404)));

	const result = await ledger.update(42, draft);

	expect(result).toEqual(sampleExpense);
	const call = calls.find((entry) => entry.method === 'PUT' && entry.url === '/api/expenses/42');
	expect(call?.body).toEqual(draft);
});

test('remove() issues a DELETE to the expense path', async () => {
	const calls = stubFetch((url) => (url === '/api/expenses/42' ? jsonResponse(undefined) : errorResponse(404)));

	await ledger.remove(42);

	const call = calls.find((entry) => entry.method === 'DELETE' && entry.url === '/api/expenses/42');
	expect(call).toBeDefined();
});

test('loadCategories() sets ledger.categories from the response', async () => {
	stubFetch((url) => (url === '/api/categories' ? jsonResponse(sampleCategories) : errorResponse(404)));

	await ledger.loadCategories();

	expect(ledger.categories).toEqual(sampleCategories);
});
