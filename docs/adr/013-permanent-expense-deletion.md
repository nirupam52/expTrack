# ADR 013: Permanently delete expenses in v1

## Status

Accepted

## Context

Users need to remove incorrect personal expense records without adding archive or audit-trail behavior.

## Decision

Delete an expense permanently after an explicit user confirmation. There is no archive, restore, or audit trail in v1.

## Consequences

- Expense queries and totals exclude deleted records without soft-delete filtering.
- The interface must make deletion clear before it occurs.
- Recovery and audit history require a future feature.
