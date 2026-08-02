# ADR 008: Allow open self-registration

## Status

Accepted

## Context

A deployment may serve its host and friends without requiring host-managed invitations.

## Decision

Any visitor may create an account on a deployment. Account approval and invitation workflows are not part of v1.

## Consequences

- The product needs a public registration flow.
- No administrator or invitation domain model is needed initially.
- Deployment operators remain responsible for choosing where to expose their installation.
