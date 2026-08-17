import { z, type ZodType } from 'zod';

const errorResponseSchema = z.object({
	message: z.string().optional(),
	error: z.string().optional(),
	detail: z.string().optional()
});
const csrfSchema = z.object({ token: z.string().min(1) });

export class HttpError extends Error {
	constructor(public readonly status: number, public readonly detail?: string) {
		super(detail ?? `Request failed with status ${status}`);
		this.name = 'HttpError';
	}
}

async function responseError(response: Response) {
	const body: unknown = response.headers.get('content-type')?.includes('json')
		? await response.json().catch(() => null)
		: null;
	const parsed = errorResponseSchema.safeParse(body);
	const candidate = parsed.success
		? parsed.data.message ?? parsed.data.error ?? parsed.data.detail
		: undefined;
	const detail = candidate && candidate.length <= 200 && !/[<>]/.test(candidate) ? candidate : undefined;
	return new HttpError(response.status, detail);
}

async function parseJson<T>(response: Response, schema: ZodType<T>) {
	const body: unknown = await response.json();
	return schema.parse(body);
}

async function fetchJson<T>(path: string, schema: ZodType<T>, init?: RequestInit): Promise<T> {
	const response = await fetch(path, { credentials: 'same-origin', ...init });
	if (!response.ok) throw await responseError(response);
	return parseJson(response, schema);
}

function requestBody(body: unknown, contentType: string): BodyInit {
	if (contentType === 'application/json') return JSON.stringify(body);
	if (body instanceof URLSearchParams) return body;
	throw new TypeError(`Unsupported request content type: ${contentType}`);
}

async function csrfToken() {
	return (await fetchJson('/api/auth/csrf', csrfSchema)).token;
}

export function get<T>(path: string, schema: ZodType<T>) {
	return fetchJson(path, schema);
}

export function post<T>(path: string, body: unknown, responseSchema: ZodType<T>, contentType?: string): Promise<T>;
export function post(path: string, body: unknown, contentType?: string): Promise<void>;
export async function post<T>(
	path: string,
	body: unknown,
	responseSchemaOrContentType?: ZodType<T> | string,
	requestedContentType = 'application/json'
): Promise<T | void> {
	const responseSchema = typeof responseSchemaOrContentType === 'string' ? undefined : responseSchemaOrContentType;
	const contentType = typeof responseSchemaOrContentType === 'string' ? responseSchemaOrContentType : requestedContentType;
	const response = await fetch(path, {
		method: 'POST',
		credentials: 'same-origin',
		headers: { 'Content-Type': contentType, 'X-CSRF-TOKEN': await csrfToken() },
		body: requestBody(body, contentType)
	});
	if (!response.ok) throw await responseError(response);
	return responseSchema ? parseJson(response, responseSchema) : undefined;
}
