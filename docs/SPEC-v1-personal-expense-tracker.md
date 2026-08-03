# Exptrack V1: Personal Expense Tracking

## Problem Statement

People need a quick, private way to record and understand their own spending on a self-hosted deployment. The current project bootstrap proves the application can run, but it does not yet provide account access, personal expense management, categories, search, or spending insight.

## Solution

Deliver a mobile-friendly personal expense tracker. Visitors can create an account and sign in with email and password. Each authenticated user manages only their own expenses, records an expense with the minimum required details, finds past expenses, and sees current-month spending and category totals.

## User Stories

1. As a visitor, I want to create an account with my email and password, so that I can keep my expenses private.
2. As a new user, I want to choose a default currency when I register, so that newly recorded expenses use the correct currency.
3. As a new user, I want a fixed set of practical categories, so that I can record an expense immediately.
4. As a user, I want to sign in with my email and password, so that I can access my data.
5. As a user, I want to sign out, so that I can end my session on a shared device.
6. As a user, I want my session stored securely in my browser, so that I do not manage access tokens myself.
7. As a user, I want to add an expense with a title, amount, category, and date, so that I can capture a purchase quickly.
8. As a user, I want the expense date to default to today, so that routine entry takes less typing.
9. As a user, I want to add a short note to an expense, so that I can preserve useful context.
10. As a user, I want invalid expense details rejected clearly, so that I can correct the entry.
11. As a user, I want to edit my expenses, so that I can correct mistakes.
12. As a user, I want explicit confirmation before permanently deleting an expense, so that I do not remove one accidentally.
13. As a user, I want to see a list of only my expenses, so that my financial data remains private.
14. As a user, I want to search expenses by title or note, so that I can find a transaction quickly.
15. As a user, I want to filter expenses by date range and category, so that I can narrow the list.
16. As a user, I want the dashboard to show my current-month total, so that I can understand recent spending at a glance.
17. As a user, I want a current-month category breakdown, so that I can see where my money is going.
18. As a user, I want to see recent expenses on the dashboard, so that I can orient myself after signing in.
19. As a user, I want a default-currency change to affect only new expenses, so that historical totals remain accurate.
20. As a user, I want every expense amount to retain its recorded currency, so that later preference changes do not rewrite history.
21. As a deployment operator, I want independent user accounts, so that several people can use the same deployment without seeing one another's data.

## Implementation Decisions

- Keep the product a modular monolith with a SvelteKit browser client, Spring Boot REST API, and SQLite database. Do not add services, CQRS, event sourcing, or generic repositories.
- Use email/password registration and sign-in. Store only strong password hashes; use secure, HttpOnly, server-side browser sessions with CSRF protection for state-changing requests.
- Derive the current user exclusively from the authenticated session. No request may select an expense or other owner by client-supplied owner ID.
- Model expenses as user-owned records containing title, positive amount, category, date, recorded currency, and optional short note. The owner is immutable.
- Define a fixed backend category taxonomy: Dining, Education, Entertainment, Fuel, Gifts & Donations, Groceries, Healthcare, Housing, Insurance, Other, Personal Care, Shopping, Subscriptions, Transportation, Travel, and Utilities.
- Store the user's default currency separately from each expense. A setting change supplies the currency for future entries only; it never rewrites historical records.
- Provide fixed category retrieval and personal expense CRUD, focused list retrieval with text, date-range, and category filters, and a current-month dashboard summary.
- Treat the REST API as the single primary test seam: the browser client consumes it, and backend HTTP tests validate authentication, ownership, validation, mutations, filtering, and dashboard responses through it.
- Maintain responsive, mobile-first interactions with small payloads, pagination for expense lists, owner-scoped queries, and database indexes supporting owner, date, category, title, and note retrieval.

## Testing Decisions

- Test externally observable behavior at the REST boundary rather than service internals or component implementation details.
- Cover registration, sign-in, sign-out, unauthenticated access, session protection, CSRF handling, and generic authentication failures.
- Cover owner isolation for every expense, list, search, filter, dashboard, update, and delete operation.
- Cover expense validation: required title/category/date, positive amount, valid fixed category, optional note, and persisted recorded currency.
- Cover fixed category retrieval.
- Cover expense create/edit/permanent delete behavior, including the UI confirmation before deletion where browser-level tests exist.
- Cover filtering and search via the list endpoint, plus current-month totals, category breakdown, and recent expenses via the dashboard endpoint.
- Use the existing HTTP health-endpoint test as the test-style precedent; add focused endpoint tests rather than a framework of unit-test doubles.

## Out of Scope

- Groups, participants, expense splitting, settlements, and any shared-expense visibility.
- Multi-currency expense entry, exchange rates, conversion, and converted reporting.
- Budgets, forecasts, recurring expenses, reports, saved searches, tags, receipts, and custom query builders.
- Invitations, account approval, email verification, social sign-in, password recovery, MFA, and outbound email.
- Custom categories, category deletion, soft deletion, restore, audit history, and bulk category reassignment.
- PostgreSQL support beyond keeping it a future option.

## Further Notes

- This specification is derived from the repository's V1 scope and accepted architectural decisions as of 2026-08-02. It intentionally supersedes the bootstrap-only milestone in the current task document for future product work.
- The primary REST seam is proposed for acceptance testing. No issue tracker or remote is configured in this checkout, so this document is the portable source until a tracker is connected; publish it as one issue and apply `ready-for-agent` then.
