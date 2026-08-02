# ADR 007: Start with a focused monthly dashboard

## Status

Accepted

## Context

The dashboard should provide immediate, useful spending insight without expanding into budgeting or forecasting.

## Decision

The v1 dashboard shows the authenticated user's current-month total spending, a category breakdown for that month, and recent expenses.

## Consequences

- Dashboard queries remain small and owner-scoped.
- Budgets, forecasts, comparison periods, and custom reports are deferred.
- The dashboard can gain additional views later without changing expense entry.
