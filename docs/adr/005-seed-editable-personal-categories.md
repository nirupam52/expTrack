# ADR 005: Seed editable personal categories

## Status

Accepted

## Context

New users need to record expenses immediately, while categories remain personal and customizable.

## Decision

Create a small starter set for each new account: Restaurants, Food, Gas, Groceries, and Entertainment. These are ordinary user-owned categories: users may rename, delete, or add categories without affecting anyone else.

## Consequences

- New users do not need to set up categories before recording expenses.
- There is no global category administration.
- Category deletion needs a defined behavior for existing expenses before that feature ships.
