# ADR 012: Require reassignment before category deletion

## Status

Accepted

## Context

Deleting a category used by expenses must not silently damage historical reporting.

## Decision

Block deletion of a category while any expense references it. The user must first edit or reassign those expenses to another category.

Bulk recategorization is deferred as a later convenience feature.

## Consequences

- Historical expenses always retain a valid category.
- Category deletion returns a clear conflict instead of changing past data.
- A future bulk-reassignment flow can be added without changing this rule.
