# Glossary

## User

An authenticated person with an account on a deployment. A user owns their personal financial data.

## Deployment

One self-hosted installation of the application. A deployment can have multiple users.

## Expense

A recorded monetary outflow owned by one user, with a title, exact amount, category, date, recorded currency, and optional note.

## Minor unit

The smallest standard unit of a currency. Expenses store their amount as a whole number of minor units so arithmetic is exact: `USD 12.34` is 1,234 cents, `JPY 500` is 500 yen, and `BHD 1.234` is 1,234 fils. The interface converts this representation to normal currency notation for people.

## Category

A fixed backend label used to organize expenses. Users cannot create, rename, or delete categories.

## Group

A future collection of users that may participate in shared expenses. Membership alone does not reveal all group expenses.

## Participant

A member of a group explicitly included in one group expense. Participants may view that expense.

## Default currency

The currency selected by a user for new v1 expenses. Changing it does not alter existing expense records.
