# ADR 011: Use secure server-side browser sessions

## Status

Accepted

## Context

The product is a browser-based application with a Spring Boot backend. It needs straightforward authentication without JWT issuance or refresh handling.

## Decision

Use server-side sessions carried in secure, HttpOnly browser cookies. Protect state-changing requests against CSRF and enforce ownership authorization on every protected request.

## Consequences

- The backend holds authentication state; the frontend does not handle bearer tokens.
- Production cookie settings require HTTPS and an appropriate SameSite policy.
- MFA and other account-security enhancements can be added later without replacing session authorization.
