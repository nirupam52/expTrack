import { expect, test, type Page } from '@playwright/test';

const categories = [{ id: 1, name: 'Groceries' }];
const session = { email: 'test@example.com', defaultCurrency: 'USD', createdAt: '2026-01-15T10:30:00Z' };
const dashboard = { month: '2026-08', currencies: [], recentExpenses: [] };

async function mockLedger(page: Page, sessionOverride: Record<string, unknown> = {}) {
	await page.route('**/api/auth/session', (route) => route.fulfill({ json: { ...session, ...sessionOverride } }));
	await page.route('**/api/categories', (route) => route.fulfill({ json: categories }));
	await page.route('**/api/expenses/dashboard', (route) => route.fulfill({ json: dashboard }));
	await page.route('**/api/auth/csrf', (route) => route.fulfill({ json: { token: 'test-token' } }));
}

async function openProfileMenu(page: Page) {
	const trigger = page.getByRole('button', { name: 'Profile' });
	await trigger.focus();
	await page.keyboard.press('ArrowDown');
	return trigger;
}

test('opens the profile menu by keyboard and navigates to account settings', async ({ page }) => {
	await mockLedger(page);

	await page.goto('/');
	const trigger = await openProfileMenu(page);
	const menu = page.getByRole('menu', { name: 'Profile' });
	await expect(menu).toBeVisible();
	await expect(page.getByRole('menuitem', { name: 'Account settings' })).toBeVisible();
	await expect(page.getByRole('menuitem', { name: 'Sign out' })).toBeVisible();
	await expect(trigger).toHaveAttribute('aria-expanded', 'true');

	await page.keyboard.press('ArrowDown');
	await expect(page.getByRole('menuitem', { name: 'Sign out' })).toBeFocused();
	await page.keyboard.press('Escape');
	await expect(menu).toHaveCount(0);
	await expect(trigger).toBeFocused();

	await page.keyboard.press('Enter');
	await page.getByRole('menuitem', { name: 'Account settings' }).click();
	await expect(menu).toHaveCount(0);
	await expect(page.getByRole('heading', { name: 'Account settings' })).toBeVisible();
	await expect(page.getByText('test@example.com')).toBeVisible();
	await expect(page.getByText('January 15, 2026')).toBeVisible();
});

test('shows Not available for accounts without a created date', async ({ page }) => {
	await mockLedger(page, { createdAt: null });

	await page.goto('/');
	await page.getByRole('button', { name: 'Profile' }).click();
	await page.getByRole('menuitem', { name: 'Account settings' }).click();

	await expect(page.getByText('Not available')).toBeVisible();
});

test('validates and saves the default currency', async ({ page }) => {
	await mockLedger(page);
	const savedCurrencies: string[] = [];
	await page.route('**/api/account/default-currency', (route) => {
		const body = route.request().postDataJSON() as { defaultCurrency: string };
		savedCurrencies.push(body.defaultCurrency);
		return route.fulfill({ json: { ...session, defaultCurrency: body.defaultCurrency } });
	});

	await page.goto('/');
	await page.getByRole('button', { name: 'Profile' }).click();
	await page.getByRole('menuitem', { name: 'Account settings' }).click();

	const currency = page.getByRole('textbox', { name: 'Currency' });
	await currency.fill('12');
	await page.getByRole('button', { name: 'Save default currency' }).click();
	await expect(page.getByRole('alert')).toHaveText('Enter a 3-letter currency code, like USD.');
	expect(savedCurrencies).toEqual([]);

	await currency.fill(' eur ');
	await page.getByRole('button', { name: 'Save default currency' }).click();
	await expect(page.getByText('Default currency saved.')).toBeVisible();
	await expect(currency).toHaveValue('EUR');
	expect(savedCurrencies).toEqual(['EUR']);
});

test('changes the password and returns to the sign-in form', async ({ page }) => {
	await mockLedger(page);
	const passwordBodies: Array<Record<string, string>> = [];
	await page.route('**/api/account/password', (route) => {
		passwordBodies.push(route.request().postDataJSON() as Record<string, string>);
		return route.fulfill({ status: 204 });
	});

	await page.goto('/');
	await page.getByRole('button', { name: 'Profile' }).click();
	await page.getByRole('menuitem', { name: 'Account settings' }).click();

	await page.getByRole('button', { name: 'Change password' }).click();
	await expect(page.getByRole('alert')).toHaveText('Enter your current password.');

	await page.getByLabel('Current password').fill('old-password-1234');
	await page.getByLabel('New password', { exact: true }).fill('short');
	await page.getByLabel('Confirm new password').fill('short');
	await page.getByRole('button', { name: 'Change password' }).click();
	await expect(page.getByRole('alert')).toHaveText('New password must be 15 to 64 characters.');

	const newPassword = 'fresh-password-5678901';
	await page.getByLabel('New password', { exact: true }).fill(newPassword);
	await page.getByLabel('Confirm new password').fill('different-password-5678');
	await page.getByRole('button', { name: 'Change password' }).click();
	await expect(page.getByRole('alert')).toHaveText('New password and confirmation do not match.');
	expect(passwordBodies).toEqual([]);

	await page.getByLabel('Confirm new password').fill(newPassword);
	await page.getByRole('button', { name: 'Change password' }).click();
	await expect(page.getByRole('status')).toHaveText('Password changed. Sign in again.');
	await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
	expect(passwordBodies).toEqual([{ currentPassword: 'old-password-1234', newPassword, newPasswordConfirmation: newPassword }]);
});
test('associates a wrong current-password response with the current-password field', async ({ page }) => {
	await mockLedger(page);
	await page.route('**/api/account/password', (route) => route.fulfill({
		status: 400,
		json: { detail: 'Current password is incorrect' }
	}));

	await page.goto('/');
	await page.getByRole('button', { name: 'Profile' }).click();
	await page.getByRole('menuitem', { name: 'Account settings' }).click();

	const currentPassword = page.getByLabel('Current password');
	const newPassword = page.getByLabel('New password', { exact: true });
	await currentPassword.fill('wrong-password-1234');
	await newPassword.fill('fresh-password-5678901');
	await page.getByLabel('Confirm new password').fill('fresh-password-5678901');
	await page.getByRole('button', { name: 'Change password' }).click();

	const error = page.getByRole('alert').filter({ hasText: 'Current password is incorrect' });
	await expect(error).toBeVisible();
	const errorId = await error.getAttribute('id');
	expect(errorId).toBeTruthy();
	await expect(currentPassword).toHaveAttribute('aria-describedby', errorId!);
	await expect(currentPassword).toHaveAttribute('aria-invalid', 'true');
	await expect(newPassword).not.toHaveAttribute('aria-describedby', errorId!);
});

test('warns before discarding account changes when navigating', async ({ page }) => {
	await mockLedger(page);

	await page.goto('/');
	await page.getByRole('button', { name: 'Profile' }).click();
	await page.getByRole('menuitem', { name: 'Account settings' }).click();
	await page.getByRole('textbox', { name: 'Currency' }).fill('EUR');

	page.once('dialog', (dialog) => dialog.dismiss());
	await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('button', { name: 'Dashboard' }).click();
	await expect(page.getByRole('heading', { name: 'Account settings' })).toBeVisible();

	page.once('dialog', async (dialog) => {
		expect(dialog.message()).toContain('Discard the changes to your account settings?');
		await dialog.accept();
	});
	await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('button', { name: 'Dashboard' }).click();
	await expect(page.getByRole('heading', { name: 'Start adding expenses to see insights here.' })).toBeVisible();
});

test('warns before signing out with unsaved account changes', async ({ page }) => {
	await mockLedger(page);

	await page.goto('/');
	await page.getByRole('button', { name: 'Profile' }).click();
	await page.getByRole('menuitem', { name: 'Account settings' }).click();
	await page.getByRole('textbox', { name: 'Currency' }).fill('EUR');

	await page.getByRole('button', { name: 'Profile' }).click();
	page.once('dialog', (dialog) => dialog.dismiss());
	await page.getByRole('menuitem', { name: 'Sign out' }).click();
	await expect(page.getByRole('heading', { name: 'Account settings' })).toBeVisible();
	await expect(page.getByRole('menu', { name: 'Profile' })).toHaveCount(0);
});
