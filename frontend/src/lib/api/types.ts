import { z } from 'zod';

const currencySchema = z.string().regex(/^[A-Z]{3}$/);

export const categorySchema = z.object({
	id: z.number().int().positive(),
	name: z.string().min(1)
});
export const categoriesSchema = z.array(categorySchema);

export const sessionSchema = z.object({
	email: z.string().email(),
	defaultCurrency: currencySchema,
	createdAt: z.iso.datetime({ offset: true }).nullable()
});

export const expenseSchema = z.object({
	id: z.number().int().positive(),
	title: z.string().min(1),
	amountMinor: z.string().regex(/^\d+$/),
	categoryId: z.number().int().positive(),
	date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
	currency: currencySchema,
	note: z.string().nullable()
});
export const expensePageSchema = z.object({
	items: z.array(expenseSchema),
	nextCursor: z.string().min(1).nullable()
});
export const dashboardCategorySchema = z.object({
	categoryId: z.number().int().positive(),
	amountMinor: z.string().regex(/^\d+$/)
});
export const dashboardCurrencySchema = z.object({
	currency: currencySchema,
	totalMinor: z.string().regex(/^\d+$/),
	categories: z.array(dashboardCategorySchema)
});
export const dashboardSchema = z.object({
	month: z.string().regex(/^\d{4}-\d{2}$/),
	currencies: z.array(dashboardCurrencySchema),
	recentExpenses: z.array(expenseSchema).max(5)
});

export type Category = z.infer<typeof categorySchema>;
export type Session = z.infer<typeof sessionSchema>;
export type Expense = z.infer<typeof expenseSchema>;
export type Dashboard = z.infer<typeof dashboardSchema>;
export type ExpenseHistoryFilters = {
	query: string;
	categoryId: number | null;
	currency: string;
	from: string;
	to: string;
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
	recordedCurrency?: string;
};

export type AccountPasswords = {
	currentPassword: string;
	newPassword: string;
	newPasswordConfirmation: string;
};
