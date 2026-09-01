import { del, get, HttpError, post, put } from '$lib/api/client';
import {
	categoriesSchema,
	expenseSchema,
	sessionSchema,
	type AccountPasswords,
	type AuthSubmission,
	type Category,
	type Expense,
	type ExpenseDraft,
	type ExpenseHistoryFilters,
	type Session
} from '$lib/api/types';

type Toast = {
	id: number;
	kind: 'success' | 'error';
	message: string;
};

type View = 'dashboard' | 'add' | 'history' | 'account';

const emptyHistoryFilters: ExpenseHistoryFilters = {
	query: '',
	categoryId: null,
	currency: '',
	from: '',
	to: ''
};

type LedgerModel = {
	session: Session | null;
	categories: Category[];
	loading: boolean;
	submitting: boolean;
	loadError: string;
	signOutError: string;
	error: string;
	editing: Expense | null;
	expenseVersion: number;
	view: View;
	savingCurrency: boolean;
	savingPassword: boolean;
	currencyError: string;
	passwordError: string;
	authNotice: string;
	historyFilters: ExpenseHistoryFilters;
	historyRemount: number;
	toast: Toast | null;
};

class LedgerPageController {
	session = $state<Session | null>(null);
	categories = $state.raw<Category[]>([]);
	loading = $state(true);
	submitting = $state(false);
	loadError = $state('');
	signOutError = $state('');
	error = $state('');
	editing = $state<Expense | null>(null);
	expenseVersion = $state(0);
	view = $state<View>('dashboard');
	formDirty = $state(false);
	accountDirty = $state(false);
	savingCurrency = $state(false);
	savingPassword = $state(false);
	currencyError = $state('');
	passwordError = $state('');
	authNotice = $state('');
	historyFilters = $state<ExpenseHistoryFilters>({ ...emptyHistoryFilters });
	// View-history calls remount the history screen; filter edits do not.
	historyRemount = $state(0);
	toast = $state<Toast | null>(null);
	private toastSequence = 0;

	readonly model: LedgerModel = createLedgerModel(this);

	loadSession = async () => {
		this.loading = true;
		this.loadError = '';
		try {
			this.session = await get('/api/auth/session', sessionSchema);
			await this.loadLedger();
		} catch (cause) {
			this.handleSessionLoadError(cause);
		}
		this.loading = false;
	};

	authenticate = async (submission: AuthSubmission) => {
		const { mode } = submission;
		this.error = '';
		this.authNotice = '';
		this.submitting = true;
		try {
			await this.submitAuthentication(submission);
			await this.loadSession();
		} catch {
			this.error = mode === 'register' ? 'Check the account details and try again.' : 'Email or password is incorrect.';
		} finally {
			this.submitting = false;
		}
	};

	addExpense = async (draft: ExpenseDraft) => {
		this.error = '';
		this.submitting = true;
		try {
			await post('/api/expenses', draft, expenseSchema);
			this.expenseVersion += 1;
			this.formDirty = false;
			return true;
		} catch (cause) {
			this.error = expenseError(cause, 'Could not save the expense. Please try again.');
			return false;
		} finally {
			this.submitting = false;
		}
	};

	updateExpense = async (draft: ExpenseDraft) => {
		if (!this.editing) return false;
		this.error = '';
		this.submitting = true;
		try {
			await put(`/api/expenses/${this.editing.id}`, draft, expenseSchema);
			this.expenseVersion += 1;
			this.formDirty = false;
			return true;
		} catch (cause) {
			this.error = expenseError(cause, 'Could not update the expense. Please try again.');
			return false;
		} finally {
			this.submitting = false;
		}
	};

	deleteExpense = async (expense: Expense) => {
		try {
			await del(`/api/expenses/${expense.id}`);
			this.finishDelete(expense);
			this.showToast('success', 'Expense deleted.');
			return true;
		} catch {
			this.showToast('error', 'Could not delete the expense.');
			return false;
		}
	};

	signOut = async () => {
		if (!this.canLeaveForm()) return;
		this.signOutError = '';
		try {
			await post('/api/auth/logout', null);
			this.resetSession();
		} catch {
			this.signOutError = 'Could not sign out. Please try again.';
		}
	};

	viewHistory = (filters: Partial<ExpenseHistoryFilters> = {}) => {
		if (!this.canLeaveForm()) return;
		this.historyFilters = { ...emptyHistoryFilters, ...filters };
		this.historyRemount += 1;
		this.editing = null;
		this.clearDirtyState();
		this.view = 'history';
	};

	startAddExpense = () => {
		if (this.view === 'add' || !this.canLeaveForm()) return;
		this.editing = null;
		this.error = '';
		this.clearDirtyState();
		this.view = 'add';
	};

	viewDashboard = () => {
		if (!this.canLeaveForm()) return;
		this.editing = null;
		this.clearDirtyState();
		this.view = 'dashboard';
	};

	openAccountSettings = () => {
		if (this.view === 'account' || !this.canLeaveForm()) return;
		this.editing = null;
		this.error = '';
		this.clearDirtyState();
		this.currencyError = '';
		this.passwordError = '';
		this.view = 'account';
	};

	saveDefaultCurrency = async (currency: string) => {
		this.currencyError = '';
		this.savingCurrency = true;
		try {
			const updated = await put('/api/account/default-currency', { defaultCurrency: currency }, sessionSchema);
			if (!updated) throw new Error('Default currency response is missing.');
			this.session = updated;
			return true;
		} catch (cause) {
			this.currencyError = accountError(cause, 'Could not save the default currency. Please try again.');
			return false;
		} finally {
			this.savingCurrency = false;
		}
	};

	changePassword = async (passwords: AccountPasswords) => {
		this.passwordError = '';
		this.savingPassword = true;
		try {
			await post('/api/account/password', passwords);
			this.resetAfterPasswordChange();
			return true;
		} catch (cause) {
			this.passwordError = accountError(cause, 'Could not change the password. Please try again.');
			return false;
		} finally {
			this.savingPassword = false;
		}
	};

	cancelEdit = () => {
		this.editing = null;
		this.error = '';
		this.formDirty = false;
		this.view = 'history';
	};

	editExpense = (expense: Expense) => {
		this.editing = expense;
		this.error = '';
		this.view = 'add';
	};

	markExpenseDirty = () => {
		this.formDirty = true;
	};

	setAccountDirty = (dirty: boolean) => {
		this.accountDirty = dirty;
	};

	updateHistoryFilters = (filters: ExpenseHistoryFilters) => {
		this.historyFilters = filters;
	};

	warnBeforeUnload = (event: BeforeUnloadEvent) => {
		if (!this.accountDirty) return;
		event.preventDefault();
		event.returnValue = '';
	};

	clearAuthError = () => {
		this.error = '';
	};

	private async loadLedger() {
		try {
			this.categories = await get('/api/categories', categoriesSchema);
		} catch {
			this.loadError = 'Unable to load your ledger. Please try again.';
		}
	}

	private handleSessionLoadError(cause: unknown) {
		if (cause instanceof HttpError && cause.status === 401) this.session = null;
		else this.loadError = 'Unable to load your ledger. Please try again.';
	}

	private async submitAuthentication({ mode, email, password, defaultCurrency }: AuthSubmission) {
		if (mode === 'register') {
			await post('/api/auth/register', { email, password, defaultCurrency: defaultCurrency.trim().toUpperCase() });
		}
		await post('/api/auth/login', new URLSearchParams({ username: email, password }), 'application/x-www-form-urlencoded');
	}

	private finishDelete(expense: Expense) {
		if (this.editing?.id === expense.id) {
			this.editing = null;
			this.error = '';
		}
		this.expenseVersion += 1;
	}

	private showToast(kind: Toast['kind'], message: string) {
		const id = ++this.toastSequence;
		this.toast = { id, kind, message };
		window.setTimeout(() => this.clearToast(id), 5000);
	}

	private clearToast(id: number) {
		if (this.toast?.id === id) this.toast = null;
	}

	private resetSession() {
		this.session = null;
		this.editing = null;
		this.view = 'dashboard';
		this.formDirty = false;
		this.accountDirty = false;
		this.authNotice = '';
	}

	private canLeaveForm() {
		if (this.view === 'add') return !this.formDirty || window.confirm('Discard the changes to this expense?');
		if (this.view === 'account') return !this.accountDirty || window.confirm('Discard the changes to your account settings?');
		return true;
	}

	private clearDirtyState() {
		this.formDirty = false;
		this.accountDirty = false;
	}

	private resetAfterPasswordChange() {
		this.session = null;
		this.view = 'dashboard';
		this.editing = null;
		this.clearDirtyState();
		this.authNotice = 'Password changed. Sign in again.';
	}
}

function createLedgerModel(controller: LedgerPageController): LedgerModel {
	const model = createCoreModel(controller);
	Object.defineProperties(model, Object.getOwnPropertyDescriptors(createAccountModel(controller)));
	Object.defineProperties(model, Object.getOwnPropertyDescriptors(createHistoryModel(controller)));
	return model as LedgerModel;
}

function createCoreModel(controller: LedgerPageController) {
	return {
		get session() { return controller.session; },
		get categories() { return controller.categories; },
		get loading() { return controller.loading; },
		get submitting() { return controller.submitting; },
		get loadError() { return controller.loadError; },
		get signOutError() { return controller.signOutError; },
		get error() { return controller.error; },
		get editing() { return controller.editing; },
		get expenseVersion() { return controller.expenseVersion; },
		get view() { return controller.view; }
	};
}

function createAccountModel(controller: LedgerPageController) {
	return {
		get savingCurrency() { return controller.savingCurrency; },
		get savingPassword() { return controller.savingPassword; },
		get currencyError() { return controller.currencyError; },
		get passwordError() { return controller.passwordError; },
		get authNotice() { return controller.authNotice; }
	};
}

function createHistoryModel(controller: LedgerPageController) {
	return {
		get historyFilters() { return controller.historyFilters; },
		get historyRemount() { return controller.historyRemount; },
		get toast() { return controller.toast; }
	};
}

function expenseError(cause: unknown, fallback: string) {
	return cause instanceof HttpError && cause.status === 400
		? `${cause.detail ? `${cause.detail}. ` : ''}Enter a title, positive amount, category, and date.`
		: fallback;
}

function accountError(cause: unknown, fallback: string) {
	return cause instanceof HttpError && cause.status === 400 && cause.detail ? cause.detail : fallback;
}

export function createLedgerPageController() {
	return new LedgerPageController();
}
