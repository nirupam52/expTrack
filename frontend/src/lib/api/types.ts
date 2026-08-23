import { z } from 'zod';

const currencySchema = z.string().regex(/^[A-Z]{3}$/);

export const categorySchema = z.object({
	id: z.number().int().positive(),
	name: z.string().min(1)
});
export const categoriesSchema = z.array(categorySchema);

export const sessionSchema = z.object({
	email: z.string().email(),
	defaultCurrency: currencySchema
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

export type Category = z.infer<typeof categorySchema>;
export type Session = z.infer<typeof sessionSchema>;
export type Expense = z.infer<typeof expenseSchema>;

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
