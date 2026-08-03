# ADR 014: Use fixed backend categories

## Status

Accepted

## Context

The editable, user-owned category model created duplicate rows for every account and requires category CRUD that is not needed for v1.

## Decision

Define one fixed taxonomy in the backend: Dining, Education, Entertainment, Fuel, Gifts & Donations, Groceries, Healthcare, Housing, Insurance, Other, Personal Care, Shopping, Subscriptions, Transportation, Travel, and Utilities.

The category endpoint returns this list from application code. Registration creates no category records, and users cannot add, rename, or delete categories. Future category additions are backend releases.

## Consequences

- The category table and its per-user seed records are removed.
- An expense will store its selected category value directly and validate new values against this taxonomy.
- `Other` covers purchases outside the current list.
- Removing or renaming a category after expenses exist is not supported, so historical values remain meaningful.
