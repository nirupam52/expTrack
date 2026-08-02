# ADR 004: Use one currency per user in v1

## Status

Accepted

## Context

Users choose a default currency during account creation and can change that setting later. Currency conversion is not yet in scope.

## Decision

Every v1 expense uses its owner's current default currency. Changing the default affects only newly created expenses; historical expenses retain their recorded currency. Individual foreign-currency expenses and conversion are deferred.

## Consequences

- Dashboard totals are exact simple sums of stored amounts.
- The currency is stored on each expense to preserve historical accuracy.
- Multi-currency entries, exchange rates, and converted reporting require a future design.
