import { expect, test, type Page } from '@playwright/test';

const categories = [{ id: 1, name: 'Groceries' }, { id: 2, name: 'Transport' }];
const session = { email: 'test@example.com', defaultCurrency: 'USD' };
const dashboard = {
	month: '2026-08',
	currencies: [{ currency: 'USD', totalMinor: '4250', categories: [{ categoryId: 1, amountMinor: '3000' }, { categoryId: 2, amountMinor: '1250' }] }],
	recentExpenses: [{ id: 1, title: 'Market', amountMinor: '3000', categoryId: 1, date: '2026-08-10', currency: 'USD', note: null }]
};

async function mockLedger(page: Page) {
	await page.route('**/api/auth/session', (route) => route.fulfill({ json: session }));
	await page.route('**/api/categories', (route) => route.fulfill({ json: categories }));
}

test('loads the dashboard', async ({ page }) => {
	await mockLedger(page);
	await page.route('**/api/expenses/dashboard', (route) => route.fulfill({ json: dashboard }));

	await page.goto('/');

	await expect(page.getByRole('heading', { name: 'August 2026' })).toBeVisible();
	await expect(page.getByText('$42.50')).toBeVisible();
	await expect(page.getByText('Market')).toBeVisible();
});

test('uses a fixed mobile dock and a wide desktop header navigation', async ({ page }) => {
	await mockLedger(page);
	await page.route('**/api/expenses/dashboard', (route) => route.fulfill({ json: dashboard }));

	await page.setViewportSize({ width: 390, height: 844 });
	await page.goto('/');

	const navigation = page.getByRole('navigation', { name: 'Main navigation' });
	await expect(navigation).toHaveCSS('position', 'fixed');
	const expense = page.locator('.recent-expenses li');
	await expect(expense).toBeVisible();
	await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
	const [navigationBox, expenseBox] = await Promise.all([navigation.boundingBox(), expense.boundingBox()]);
	expect(navigationBox).not.toBeNull();
	expect(expenseBox).not.toBeNull();
	expect(expenseBox!.y + expenseBox!.height).toBeLessThanOrEqual(navigationBox!.y);

	await page.setViewportSize({ width: 1440, height: 900 });
	const [desktopNavigationBox, headerBox] = await Promise.all([navigation.boundingBox(), page.locator('header').boundingBox()]);
	expect(desktopNavigationBox).not.toBeNull();
	expect(headerBox).not.toBeNull();
	expect(desktopNavigationBox!.y).toBeGreaterThanOrEqual(headerBox!.y);
	expect(desktopNavigationBox!.y + desktopNavigationBox!.height).toBeLessThanOrEqual(headerBox!.y + headerBox!.height);
	expect(await page.locator('.app').evaluate((element) => element.getBoundingClientRect().width)).toBeGreaterThan(672);
});

test('uses dark native controls and full-size text actions', async ({ page }) => {
	await mockLedger(page);
	await page.route('**/api/expenses/dashboard', (route) => route.fulfill({ json: dashboard }));
	await page.route(/\/api\/expenses(?:\?.*)?$/, (route) => route.fulfill({ json: { items: dashboard.recentExpenses, nextCursor: null } }));

	await page.goto('/');
	await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('button', { name: 'History' }).click();
	await expect(page.getByRole('heading', { name: 'Expense history' })).toBeVisible();
	await page.getByRole('button', { name: 'Delete', exact: true }).click();

	expect(await page.locator('html').evaluate((element) => getComputedStyle(element).colorScheme)).toBe('dark');
	const heights = await page.locator('button, input, select').evaluateAll((elements) => elements.map((element) => element.getBoundingClientRect().height));
	expect(heights.every((height) => height >= 44)).toBe(true);
});

test('prioritises adding an expense in the desktop overview', async ({ page }) => {
	await mockLedger(page);
	await page.route('**/api/expenses/dashboard', (route) => route.fulfill({ json: dashboard }));

	await page.setViewportSize({ width: 1440, height: 900 });
	await page.goto('/');

	const addExpense = page.getByRole('button', { name: 'Add expense from dashboard' });
	await expect(addExpense).toBeVisible();
	const [summaryBox, recentBox] = await Promise.all([
		page.getByRole('region', { name: 'Total spending in USD' }).boundingBox(),
		page.getByRole('region', { name: 'Latest expenses' }).boundingBox()
	]);
	expect(summaryBox).not.toBeNull();
	expect(recentBox).not.toBeNull();
	expect(recentBox!.x).toBeGreaterThan(summaryBox!.x);
	expect(recentBox!.y).toBeLessThanOrEqual(summaryBox!.y + 1);

	await addExpense.click();
	await expect(page.getByRole('heading', { name: 'Add an expense' })).toBeVisible();
});

test('shows a dashboard error and retries', async ({ page }) => {
	let requests = 0;
	await mockLedger(page);
	await page.route('**/api/expenses/dashboard', (route) => {
		requests += 1;
		return requests === 1 ? route.fulfill({ status: 500, json: { detail: 'Unavailable' } }) : route.fulfill({ json: dashboard });
	});

	await page.goto('/');
	await expect(page.getByRole('alert')).toHaveText('Could not load your dashboard. Please try again.');
	await page.getByRole('button', { name: 'Retry' }).click();
	await expect(page.getByRole('heading', { name: 'August 2026' })).toBeVisible();
	expect(requests).toBe(2);
});

test('opens filtered history from a dashboard category', async ({ page }) => {
	await mockLedger(page);
	await page.route('**/api/expenses/dashboard', (route) => route.fulfill({ json: dashboard }));
	await page.route('**/api/expenses?**', (route) => {
		const url = new URL(route.request().url());
		expect(url.searchParams.get('categoryId')).toBe('1');
		expect(url.searchParams.get('currency')).toBe('USD');
		expect(url.searchParams.get('from')).toBe('2026-08-01');
		expect(url.searchParams.get('to')).toBe('2026-08-31');
		return route.fulfill({ json: { items: [dashboard.recentExpenses[0]], nextCursor: null } });
	});

	await page.goto('/');
	await page.getByRole('button', { name: 'View Groceries expenses for August 2026 in USD' }).click();

	await expect(page.getByRole('heading', { name: 'Expense history' })).toBeVisible();
	await expect(page.getByRole('combobox', { name: 'Category' })).toHaveValue('1');
	await expect(page.getByText('USD - 1 shown')).toBeVisible();
	await expect(page.getByRole('textbox', { name: 'From' })).toHaveValue('2026-08-01');
	await expect(page.getByRole('textbox', { name: 'To' })).toHaveValue('2026-08-31');
});

test('keeps applied history filters after editing an expense', async ({ page }) => {
	await mockLedger(page);
	const historyQueries: Array<string | null> = [];
	await page.route('**/api/auth/csrf', (route) => route.fulfill({ json: { token: 'test-token' } }));
	await page.route('**/api/expenses*', (route) => {
		const url = new URL(route.request().url());
		historyQueries.push(url.searchParams.get('query'));
		return route.fulfill({ json: { items: [dashboard.recentExpenses[0]], nextCursor: null } });
	});
	await page.route('**/api/expenses/dashboard', (route) => route.fulfill({ json: dashboard }));
	await page.route('**/api/expenses/1', (route) => route.fulfill({ json: dashboard.recentExpenses[0] }));

	await page.goto('/');
	await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('button', { name: 'History' }).click();
	await page.getByRole('textbox', { name: 'Search title or note' }).fill('Market');
	await page.getByRole('button', { name: 'Apply filters' }).click();
	await page.getByRole('button', { name: 'Edit' }).click();
	await page.getByRole('button', { name: 'Cancel' }).click();

	await expect(page.getByRole('textbox', { name: 'Search title or note' })).toHaveValue('Market');
	await page.getByRole('button', { name: 'Edit' }).click();
	await page.getByRole('button', { name: 'Save changes' }).click();
	await expect(page.getByRole('textbox', { name: 'Search title or note' })).toHaveValue('Market');
	// One fetch on entering history, one per apply, one per return after cancel and after save.
	expect(historyQueries).toEqual([null, 'Market', 'Market', 'Market']);
});

test('distinguishes no current-month spending from no expenses', async ({ page }) => {
	await mockLedger(page);
	await page.route('**/api/expenses/dashboard', (route) => route.fulfill({ json: { ...dashboard, currencies: [] } }));

	await page.goto('/');

	await expect(page.getByRole('heading', { name: 'No spending this month yet.' })).toBeVisible();
	await expect(page.getByRole('heading', { name: 'Start adding expenses to see insights here.' })).toHaveCount(0);
	await expect(page.getByRole('button', { name: 'Add an expense' })).toBeVisible();
	await expect(page.getByText('Market')).toBeVisible();
});
