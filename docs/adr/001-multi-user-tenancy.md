# ADR 001: Support independent user accounts

## Status

Accepted

## Context

A deployment may be hosted by one person but used by their friends, like a small SaaS application.

## Decision

The application supports multiple independent user accounts. Expenses and all future personal financial data belong to one user and must be authorized against that user on every request. The fixed category taxonomy is application configuration, not personal data.

Sharing expenses between users is not part of the initial model; it remains a future groups feature.

## Consequences

- Authentication is required before expense features ship.
- The backend must derive the current user from authentication, never accept an owner ID from the client.
- Database queries and unique constraints must be scoped to the owner where applicable.
