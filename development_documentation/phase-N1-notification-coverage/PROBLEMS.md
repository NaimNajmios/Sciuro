# Problems: Phase N1 — Notification Coverage

## P1 — capture-layer: bigText/textLines content silently dropped

**Status:** Fixed 27 Jul 2026

### Symptom

Maybank2u Scan & Pay notifications appeared in the Pipeline Trace tab as `CAPTURE → DROP {reason=blank_content}`. Gmail forwards of Maybank notifications reached `PARSE_LLM → FAILURE` with the LLM reporting "lacks sufficient information" — but the email body visible in the Gmail app was a normal notification forward with full transaction details.

### Root cause

`SciuroNotificationService.kt` only read `Notification.EXTRA_TEXT` (the collapsed one-line preview). Maybank2u's Scan & Pay flow posts with `EXTRA_TEXT` empty and the real content only in `EXTRA_BIG_TEXT`. Gmail posts with only a short preview in `EXTRA_TEXT` and the full body in `EXTRA_TEXT_LINES`. Neither field was ever read by the service.

### Why it took real screenshots to find

The `blank_content` drop trace included only `{"reason":"blank_content","package":"com.maybank2u.life"}` — no indication of which extras keys were present. A developer seeing this trace in isolation would need to either:
1. Read the source code and deduce which fields are being read
2. Install the app and inspect notifications manually

Neither is acceptable for a five-minute fix.

### Fix

1. `resolveText(notification)` reads all three fields with priority `bigText > textLines > shortText`.
2. The `blank_content` drop trace now logs `extras_present` (e.g. `"android.title,android.bigText"`), making the next occurrence of this pattern legible immediately.
3. Added 3 regression fixtures to `FixtureLibrary` reproducing the exact captured payloads.

### Diagnostic lesson

When a drop/parse-failure trace lacks field-level metadata, each new occurrence requires a source-code read to understand what happened. The `extras_present` field closes this gap for notification content — future developers (or the current developer debugging a different app's similar pattern) see `"extras_present":"android.title,android.bigText"` and know instantly the content was there but not in the field being read.

## P2 — capture-layer: notifications with content in custom extras silently dropped

**Status:** Fixed 28 Jul 2026

### Symptom

Maybank2u "Scan & Pay" notifications still appeared as `CAPTURE → DROP {reason=blank_content}` even after the bigText/textLines fix. Pipeline Trace's `extras_present` showed `"android.title,android.subText"` — helpful diagnostic, but the standard extras fields genuinely were blank. The notification's full transaction content was in a custom extra key `full_desc` that no code path ever inspected.

### Root cause

`SciuroNotificationService.resolveText()` only read three standard Android notification extras (`EXTRA_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_TEXT_LINES`). Maybank2u's Scan & Pay flow posts with all three standard fields blank and the real content only in the custom key `full_desc`. No fallback existed for custom extras.

### Why it wasn't caught in N1

The N1 bigText/textLines fix was tested against notifications where content was in standard extras. The Maybank2u Scan & Pay case uses custom extras — a distinct pattern that required a different code path. The `extras_present` diagnostic added in N1 was critical in identifying this: seeing `"android.subText"` (not a field being read) confirmed the content was somewhere other than the three standard fields.

### Fix

1. Added `KNOWN_CONTENT_KEYS` map for known package→key mappings (e.g. `com.maybank2u.life → full_desc`).
2. Added `EXCLUDED_KEYS` set and `android.*` prefix filter to avoid picking up binary, timing, or noise keys.
3. Added generic scan gated by `AggregatorHeuristicFilter.isFinancial()` — catches any app using custom extras for financial content, not just listed packages.
4. Architecture: `Bundle` inspection stays in androidMain (`resolveFromExtras`); scanning logic in commonMain (`resolveCustomExtrasFallback`) for testability.

### Adjacent: SmsReceiver keyword filter divergence

## P3 — merchant regex truncates names containing abbreviation periods

**Status:** Fixed 28 Jul 2026

### Symptom

The Maybank2u Scan & Pay fixture outflow merchant `"SITI FIKRIYAH BINTI I.R A"` was being truncated to `"SITI FIKRIYAH BINTI I"`. The "I.R" abbreviation (likely initials for "Ishak Rasheed" or similar) was cut at the first period.

### Root cause

Both `outflowMerchantRegex` and `inflowMerchantRegex` in `RegexExtractors.kt` used a bare `\.` as a sentence-ending terminator alternation in the non-capturing group. The regex engine cannot distinguish an abbreviation period (I.R) from a sentence-ending period (A.) — both match `\.`, causing the lazy quantifier `+?` to stop before the abbreviation period is consumed.

### Fix

Changed `\.` to `\.(?=\s|$)` — a period followed by whitespace or end-of-string is treated as a sentence terminator; a period followed by a letter (as in "I.R") is consumed as part of the merchant name. Both outflow and inflow regexes fixed since the same failure mode existed in both.

### Why both regexes needed fixing

The outflow regex catches "to/at/kepada SITI FIKRIYAH BINTI I.R A" and the inflow regex catches "from SITI FIKRIYAH BINTI I.R ABDUL KHAWI". Both patterns use the same bare `\.` terminator with the same vulnerability. The bug was confirmed in both during testing. `inflowMerchantRegex` was not touched in the original Claude proposal but was correctly added during implementation.

`SmsReceiver.kt` had its own `hasFinancialSignal` check (7 keywords, substring match) while `AggregatorHeuristicFilter` (shared, used by SciuroNotificationService) had 22 keywords with word-boundary regex. The two checks could produce different results for the same financial content. Consolidated to `AggregatorHeuristicFilter.isFinancial()` — both callers now use the same filter.
