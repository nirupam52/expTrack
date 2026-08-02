# ADR 006: Use a minimal personal-expense form

## Status

Accepted

## Context

Recording an expense must be fast enough to use routinely.

## Decision

A v1 expense has:

- title (required)
- amount (required)
- category (required)
- date (required, defaulting to today)
- note (optional, short free text)

The user may replace the default date before saving.

## Consequences

- The initial form contains no payer, participants, receipt, tags, or split fields.
- Validation must reject missing titles, non-positive amounts, invalid categories, and invalid dates.
