# ADR 015: Store expense money in integer minor units

## Status

Accepted

## Context

An expense amount must remain exact when it is recorded, summed, displayed, and retained as history. Binary floating-point values cannot represent many ordinary decimal amounts exactly: for example, a value entered as `12.34` can become a nearby approximation. Repeated arithmetic on those approximations can produce incorrect totals.

The application supports a user's selected ISO currency and preserves that currency on each expense. Currencies do not all use two decimal places: United States dollars use cents, Japanese yen normally have no fractional unit, and Bahraini dinars use three fractional digits. A fixed cents-only convention would therefore be incorrect.

## Decision

Store every expense amount as a positive integer number of the recorded currency's minor units, with the ISO currency code stored on the same expense record.

Examples:

| User-visible amount | Currency | Stored integer amount |
| --- | --- | --- |
| `$12.34` | `USD` | `1234` cents |
| `¥500` | `JPY` | `500` yen |
| `1.234 BD` | `BHD` | `1234` fils |

The user interface accepts and displays normal currency amounts. Conversion between that display value and the stored integer happens at the application boundary using the recorded currency's standard fraction digits. The backend validates that an entered amount is positive, has no more fractional precision than that currency permits, and can be converted without loss. It does not round a submitted amount silently.

The database column is an integer, never a floating-point column. Totals sum those integers only when all included expenses have the same recorded currency; v1 does not convert between currencies.

## Consequences

- Expense amounts and same-currency dashboard totals are exact.
- Historical amounts remain interpretable after a user changes their default currency because each record retains its currency code.
- The expense API and UI need a small, explicit conversion at their boundary; the database does not contain formatted currency strings or decimal approximations.
- Currency conversion, custom precision rules, and mixed-currency totals remain out of scope for v1.
