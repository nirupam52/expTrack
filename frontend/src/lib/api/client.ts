export class HttpError extends Error {
	constructor(public readonly status: number, public readonly detail?: string) {
		super(detail ?? `Request failed with status ${status}`);
		this.name = 'HttpError';
	}
}

async function responseError(response: Response) {
	let detail: string | undefined;
	if (response.headers.get('content-type')?.includes('json')) {
		const body: unknown = await response.json().catch(() => null);
		if (body && typeof body === 'object' && !Array.isArray(body)) {
			const { message, error, detail: problemDetail } = body as { message?: unknown; error?: unknown; detail?: unknown };
			const candidate = typeof message === 'string' ? message : typeof error === 'string' ? error : typeof problemDetail === 'string' ? problemDetail : undefined;
			detail = candidate && candidate.length <= 200 && !/[<>]/.test(candidate) ? candidate : undefined;
		}
	}
	return new HttpError(response.status, detail);
}

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
	const response = await fetch(path, { credentials: 'same-origin', ...init });
	if (!response.ok) throw await responseError(response);
	return response.json() as Promise<T>;
}

async function csrfToken() {
	return (await fetchJson<{ token: string }>('/api/auth/csrf')).token;
}

export function get<T>(path: string) {
	return fetchJson<T>(path);
}

export async function post<T = void>(path: string, body: unknown, contentType = 'application/json'): Promise<T> {
	const response = await fetch(path, {
		method: 'POST',
		credentials: 'same-origin',
		headers: { 'Content-Type': contentType, 'X-CSRF-TOKEN': await csrfToken() },
		body: contentType === 'application/json' ? JSON.stringify(body) : body as BodyInit
	});
	if (!response.ok) throw await responseError(response);
	return response.headers.get('content-type')?.includes('application/json') ? response.json() as Promise<T> : undefined as T;
}
