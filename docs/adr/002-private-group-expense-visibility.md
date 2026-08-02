# ADR 002: Restrict group-expense visibility to relevant users

## Status

Accepted

## Context

Personal data must remain private. Future groups allow shared expenses without exposing unrelated transactions.

## Decision

Personal expenses are visible only to their owner. A group expense is visible only to its creator and the users listed as participants. Group membership alone does not grant access to every group expense.

All participants must be members of the expense's group. The API must enforce visibility server-side for reads, updates, and deletes.

## Consequences

- The future group-expense model needs explicit participants.
- List and dashboard queries must filter by the authenticated user's relevance to each expense.
- Group membership changes must not reveal unrelated historical expenses.
