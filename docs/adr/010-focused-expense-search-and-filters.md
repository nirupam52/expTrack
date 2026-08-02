# ADR 010: Keep expense retrieval focused

## Status

Accepted

## Context

Users need to find personal expenses without the complexity of a reporting system.

## Decision

The expense list supports text search over title and note, plus date-range and category filters. All results are limited to the authenticated user's expenses.

## Consequences

- The initial indexes and queries target owner, date, category, title, and note retrieval.
- Saved searches, arbitrary query builders, and filters for future group data are deferred.
