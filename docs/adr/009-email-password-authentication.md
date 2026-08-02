# ADR 009: Use email and password authentication in v1

## Status

Accepted

## Context

Users need a familiar way to register and sign in without adding third-party identity providers.

## Decision

Use email and password for registration and sign-in. Email verification, social sign-in, and password-reset flows are deferred.

Passwords must be stored only as strong password hashes; authentication must establish the user used for all authorization checks.

## Consequences

- The initial system has no OAuth integration or outbound mail service.
- Account recovery needs a future design before it is offered.
- Login and registration require rate limiting and generic authentication failure responses.
