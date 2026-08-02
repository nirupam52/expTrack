# ADR 003: Ship personal expenses before groups

## Status

Accepted

## Context

The product ultimately supports private shared expenses, but its immediate milestone is project bootstrap and the core personal expense experience.

## Decision

Build only personal expenses initially. Each expense has one immutable owner, established from the authenticated request. Do not add group, participant, split, or settlement tables or API fields until the groups feature is scheduled.

## Consequences

- The initial expense schema and queries stay simple and owner-scoped.
- Groups later extend the domain with separate tables and authorization rules from ADR 002.
- Existing personal-expense behavior remains unchanged when groups are introduced.
