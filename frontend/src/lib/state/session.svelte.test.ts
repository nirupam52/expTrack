import { afterEach, beforeEach, expect, test, vi } from 'vitest';

import { errorResponse, jsonResponse, stubFetch } from '../api/fetch-stub.ts';
import { session } from './session.svelte.ts';
import type { Session } from '../api/types.ts';

const sampleSession: Session = {
	email: 'ada@example.com',
	defaultCurrency: 'USD',
	createdAt: '2024-01-15T10:30:00Z'
};

beforeEach(() => {
	session.current = null;
});

afterEach(() => {
	vi.unstubAllGlobals();
});

test('load() sets session.current from a successful response', async () => {
	stubFetch((url) => (url === '/api/auth/session' ? jsonResponse(sampleSession) : errorResponse(404)));

	await session.load();

	expect(session.current).toEqual(sampleSession);
});

test('load() resolves to a signed-out state on 401 without throwing', async () => {
	stubFetch((url) => (url === '/api/auth/session' ? errorResponse(401) : errorResponse(404)));

	await expect(session.load()).resolves.toBeUndefined();
	expect(session.current).toBeNull();
});

test('load() rethrows on a non-401 failure', async () => {
	stubFetch((url) => (url === '/api/auth/session' ? errorResponse(500) : errorResponse(404)));

	await expect(session.load()).rejects.toThrow();
});

test('authenticate() with sign-in issues exactly one login POST and no register POST', async () => {
	const calls = stubFetch((url) => (url === '/api/auth/login' ? jsonResponse(null) : errorResponse(404)));

	await session.authenticate({ mode: 'sign-in', email: 'ada@example.com', password: 'hunter2', defaultCurrency: 'usd' });

	const registerCalls = calls.filter((call) => call.url === '/api/auth/register');
	const loginCalls = calls.filter((call) => call.url === '/api/auth/login');
	expect(registerCalls).toHaveLength(0);
	expect(loginCalls).toHaveLength(1);
	expect(loginCalls[0].method).toBe('POST');
});

test('authenticate() with register posts to register before login', async () => {
	const calls = stubFetch((url) =>
		url === '/api/auth/register' || url === '/api/auth/login' ? jsonResponse(null) : errorResponse(404)
	);

	await session.authenticate({ mode: 'register', email: 'ada@example.com', password: 'hunter2', defaultCurrency: 'usd' });

	const mutationCalls = calls.filter((call) => call.method === 'POST');
	const registerIndex = mutationCalls.findIndex((call) => call.url === '/api/auth/register');
	const loginIndex = mutationCalls.findIndex((call) => call.url === '/api/auth/login');
	expect(registerIndex).toBeGreaterThanOrEqual(0);
	expect(loginIndex).toBeGreaterThan(registerIndex);
});

test('signOut() clears session.current', async () => {
	session.current = sampleSession;
	stubFetch((url) => (url === '/api/auth/logout' ? jsonResponse(null) : errorResponse(404)));

	await session.signOut();

	expect(session.current).toBeNull();
});

test('saveDefaultCurrency() replaces session.current with the returned session', async () => {
	session.current = sampleSession;
	const updated: Session = { ...sampleSession, defaultCurrency: 'EUR' };
	stubFetch((url) => (url === '/api/account/default-currency' ? jsonResponse(updated) : errorResponse(404)));

	await session.saveDefaultCurrency('EUR');

	expect(session.current).toEqual(updated);
});

test('changePassword() clears session.current', async () => {
	session.current = sampleSession;
	stubFetch((url) => (url === '/api/account/password' ? jsonResponse(null) : errorResponse(404)));

	await session.changePassword({ currentPassword: 'hunter2', newPassword: 'hunter3', newPasswordConfirmation: 'hunter3' });

	expect(session.current).toBeNull();
});
