import { vi } from 'vitest';

export type FetchCall = { url: string; method: string; body: unknown };

export function jsonResponse(body: unknown, status = 200) {
	return { ok: true, status, headers: { get: () => 'application/json' }, json: async () => body };
}

export function errorResponse(status: number) {
	return { ok: false, status, headers: { get: () => null } };
}

/** Stubs `fetch`, transparently satisfying the CSRF preflight every mutation issues, and records every call. */
export function stubFetch(responder: (url: string, method: string, body: unknown) => unknown) {
	const calls: FetchCall[] = [];
	vi.stubGlobal(
		'fetch',
		vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
			const url = String(input);
			const method = init?.method ?? 'GET';
			const rawBody = init?.body;
			const body = typeof rawBody === 'string' ? JSON.parse(rawBody) : rawBody;
			calls.push({ url, method, body });
			if (url.includes('/api/auth/csrf')) return jsonResponse({ token: 'test-token' });
			return responder(url, method, body);
		})
	);
	return calls;
}
