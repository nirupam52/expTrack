export type Category = { id: number; name: string };

export type Session = { email: string; defaultCurrency: string };

export type Expense = {
	id: number;
	title: string;
	amountMinor: string;
	categoryId: number;
	date: string;
	currency: string;
	note: string | null;
};

export type AuthSubmission = {
	mode: 'sign-in' | 'register';
	email: string;
	password: string;
	defaultCurrency: string;
};

export type ExpenseDraft = {
	title: string;
	amount: string;
	categoryId: number | null;
	date: string;
	note: string;
};
