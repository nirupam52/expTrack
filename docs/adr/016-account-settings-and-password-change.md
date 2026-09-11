# ADR 016: Provide account settings and password change

## Status

Accepted

## Context

Users need one fast, clear place to view account information and change personal preferences. The existing account model has an email address and a default currency. It does not yet record when the account was created.

## Decision

Use a profile button in the application header. It opens a menu of account actions. The Account settings item opens a dedicated Account page, which shows the account email address, account creation date when available, and current default currency. Sign out is a menu action.

The Account page lets the authenticated user change the default currency and password. Default-currency changes require an explicit save and never change an open expense form. The password form has current password, new password, and new-password confirmation fields.

Use the current NIST SP 800-63B length and composition guidance for single-factor passwords: a minimum of 15 characters, support for up to 64 characters, Unicode support, no composition rules, and no periodic password expiry. A wrong current password shows a field error. Common-password and breach blocklist validation is deferred.

After a successful password change, invalidate every active session for that user, take the user to sign in, and show a success message.

Store an immutable account creation timestamp for new user accounts. Keep the timestamp unavailable for existing accounts because their true creation time is not known.

Warn before leaving the Account page with a changed but unsaved currency selection or typed password. Keep password values only in browser memory.

## Consequences

- The user account data model needs an optional account creation timestamp and a migration that leaves existing users without a fabricated date.
- Default-currency changes use an explicit save action, preserve every expense's recorded currency, and do not change an open expense form.
- Password changes require protected API support, current-password and confirmation validation, and user-wide session invalidation. Password validation stays in one focused module so a blocklist can be added later.
- The profile menu provides compact access to Account settings and sign out. Account settings opens the separate Account page.
