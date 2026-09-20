import { get, post, put, HttpError } from '../api/client';
import { sessionSchema, type AccountPasswords, type AuthSubmission, type Session } from '../api/types';

/**
 * The seam for everything about the authenticated account: who is signed in,
 * signing in/out/up, and the account settings that live on the session (default
 * currency, password). Callers read `session.current` reactively and call the
 * methods below instead of hitting `$lib/api/client` directly.
 */
class SessionStore {
	current = $state<Session | null>(null);

	/**
	 * Loads the current session. Resolves to a signed-out state (`current = null`)
	 * on 401; any other failure is rethrown for the caller to report.
	 */
	async load(): Promise<void> {
		try {
			this.current = await get('/api/auth/session', sessionSchema);
		} catch (cause) {
			if (cause instanceof HttpError && cause.status === 401) {
				this.current = null;
				return;
			}
			throw cause;
		}
	}

	/** Registers (if requested) and signs in. Does not reload the session. */
	async authenticate({ mode, email, password, defaultCurrency }: AuthSubmission): Promise<void> {
		if (mode === 'register') {
			await post('/api/auth/register', { email, password, defaultCurrency: defaultCurrency.trim().toUpperCase() });
		}
		await post('/api/auth/login', new URLSearchParams({ username: email, password }), 'application/x-www-form-urlencoded');
	}

	async signOut(): Promise<void> {
		await post('/api/auth/logout', null);
		this.current = null;
	}

	/** Saves the account's default currency and updates the current session. */
	async saveDefaultCurrency(currency: string): Promise<void> {
		this.current = await put('/api/account/default-currency', { defaultCurrency: currency }, sessionSchema);
	}

	/** Changes the password. Every session for the account is invalidated server-side. */
	async changePassword(passwords: AccountPasswords): Promise<void> {
		await post('/api/account/password', passwords);
		this.current = null;
	}
}

export const session = new SessionStore();
