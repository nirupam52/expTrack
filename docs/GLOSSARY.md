# Glossary

## User

An authenticated person with an account on a deployment. A user owns their personal financial data.

## Deployment

One self-hosted installation of the application. A deployment can have multiple users.

## Expense

A recorded monetary outflow owned by one user, with a title, exact amount, category, date, recorded currency, and optional note.

## Minor unit

The smallest standard unit of a currency. Expenses store their amount as a whole number of minor units so arithmetic is exact: `USD 12.34` is 1,234 cents, `JPY 500` is 500 yen, and `BHD 1.234` is 1,234 fils. The interface converts this representation to normal currency notation for people.

## Expense date

The calendar date a user attributes to an expense. It represents when the outflow occurred, not when the application received the record.

## Expense history

The personal, date-ordered list of a user's expenses. It can be narrowed by title or note text, category, and an inclusive expense-date range.

## Recorded currency

The ISO currency captured with an expense when it is created. It remains the expense's currency when the user's default currency changes or the expense is edited.

## Permanent deletion

The irreversible removal of an expense. A deleted expense no longer appears in expense history or spending totals and cannot be restored in v1.

## Category

A fixed backend label used to organize expenses. Users cannot create, rename, or delete categories.

## Group

A future collection of users that may participate in shared expenses. Membership alone does not reveal all group expenses.

## Participant

A member of a group explicitly included in one group expense. Participants may view that expense.

## Default currency

The currency selected by a user for new v1 expenses. Changing it does not alter existing expense records.
