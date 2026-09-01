import { expect, test, type Page } from '@playwright/test';

const passwordError = 'Password must be between 15 and 64 characters';

async function mockUnauthenticatedAuth(page: Page) {
	await page.route('**/api/auth/session', (route) => route.fulfill({ status: 401, json: { message: 'Unauthenticated' } }));
	await page.route('**/api/auth/csrf', (route) => route.fulfill({ json: { token: 'test-token' } }));
}

async function openRegistration(page: Page) {
	await page.goto('/');
	await expect(page.getByRole('heading', { name: 'Know where it went.' })).toBeVisible();
	await page.getByRole('button', { name: 'Create an account' }).click();
	await page.getByLabel('Email').fill('new@example.com');
	await page.getByLabel('Default currency').fill('USD');
}

async function expectRegistrationBlocked(page: Page, password: string) {
	let registrationRequests = 0;
	await page.route('**/api/auth/register', (route) => {
		registrationRequests += 1;
		return route.fulfill({ status: 500, json: { message: 'Unexpected registration request' } });
	});

	const passwordInput = page.getByLabel('Password');
	await passwordInput.fill(password);
	await page.getByRole('button', { name: 'Create account' }).click();

	const error = page.locator('#password-error');
	await expect(error).toHaveText(passwordError);
	await expect(passwordInput).toHaveAttribute('aria-describedby', 'password-error');
	await expect(passwordInput).toHaveAttribute('aria-invalid', 'true');
	expect(registrationRequests).toBe(0);
}

test('blocks registration passwords shorter than 15 Unicode code points', async ({ page }) => {
	await mockUnauthenticatedAuth(page);
	await openRegistration(page);
	await expectRegistrationBlocked(page, '😀'.repeat(8));
});

test('blocks registration passwords longer than 64 Unicode code points', async ({ page }) => {
	await mockUnauthenticatedAuth(page);
	await openRegistration(page);
	await expectRegistrationBlocked(page, 'a'.repeat(65));
});

test('shows the server error when sign-in fails', async ({ page }) => {
	await mockUnauthenticatedAuth(page);
	await page.route('**/api/auth/login', (route) => route.fulfill({ status: 401, body: '' }));
	await page.goto('/');
	await page.getByLabel('Email').fill('alice@example.com');
	await page.getByLabel('Password').fill('wrong-password');
	await page.getByRole('button', { name: 'Sign in' }).click();

	await expect(page.getByRole('alert')).toHaveText('Email or password is incorrect.');
});
