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

### Adjacent: SmsReceiver keyword filter divergence

`SmsReceiver.kt` had its own `hasFinancialSignal` check (7 keywords, substring match) while `AggregatorHeuristicFilter` (shared, used by SciuroNotificationService) had 22 keywords with word-boundary regex. The two checks could produce different results for the same financial content. Consolidated to `AggregatorHeuristicFilter.isFinancial()` — both callers now use the same filter.
