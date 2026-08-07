# Phase U1 — User-Facing Ingestion Visibility — TEST_NOTES

## Summary
The capture → parse → classify → book pipeline is now visible to the user. A persistent capture-status indicator on the Dashboard hero (dot + freshness label, tappable into an Activity Log), a source badge on `TransactionCard` (Auto / AI / Manual), and a one-time banner for financial notifications that fail to parse.

## What was tested

### Ingestion outcome classification (core-parsing, core-classifier, core-ledger)
- [PASS] `SciuroParserPipelineTest` (8 tests): confident regex success, LLM success, low-confidence-deterministic + LLM wins, LLM-null fallback to deterministic (untrusted), no-key untrusted fallback, both-null → `ParseResult.Failure`, LLM `not_a_transaction` → `ParseResult.NotATransaction`.
- [PASS] `SciuroIngestionOrchestratorTest` (3 tests): stranded-event recovery, dead-letter-on-null (now handled inside the booking use case, not the orchestrator), realtime event processing.
- [PASS] `RawEventActivityTest` (4 jvmTest): `observeLastCapturedAt()` (null when empty, value after insert), `markActivityStatus` persistence, `observeRecentActivity(10)` excludes IGNORED and orders by capture time desc, `observeUnacknowledgedParseFailures` surfaces `DROPPED`/`user_alerted_at IS NULL` and stops surfacing after `acknowledgeActivityAlert`.
- [PASS] `IngestionFreshnessTest` (6 jvmTest): listener-disabled → OFF, never-captured → OFF, ≤5 min → FRESH, >5 min ≤24 h → STALE, >24 h → OFF, future-dated → FRESH.
- `DefaultTransactionBookingUseCase` now maps outcomes to `activity_status`: `PARSED` (booked + reviewed), `NEEDS_REVIEW` (booked + unreviewed), `IGNORED` (not-a-transaction, duplicate, non-financial parse failure), `DROPPED` (financial parse failure, invalid amount, unknown direction) with a sanitized `activity_reason`.

### Duplicate dead-letter fix
- [PASS] The orchestrator no longer dead-letters every `null` booking result. Duplicates are `markProcessed` + `IGNORED` inside the booking use case (verified by the updated orchestrator test asserting the use case owns the terminal state).

### UI
- [VERIFIED (compile + assemble)] Dashboard hero `CaptureStatusRow` (dot green/amber/red + freshness label, tap → Activity Log), `ParseFailureBanner` (Add → prefilled FastTransactionSheet, dismiss → acknowledge), `ActivityLogScreen` (last 10 events with Parsed/Needs Review/Dropped status, app-label resolution, sanitized drop reason), `TransactionCard` source badge (Auto/AI/Manual with confidence in the accessibility label), navigation route `activity_log` mapped to the Dashboard tab.
- [NOT DEVICE-TESTED] Notification access state refresh on `ON_RESUME` (uses the same `Settings.Secure` check as `MainActivity`). Banner persistence across restarts (driven by `user_alerted_at` column) not exercised on a real device.

## Test commands
```
./gradlew :core-ledger:jvmTest --console=plain
./gradlew :core-parsing:testDebugUnitTest --tests "com.sciuro.core.parsing.engine.SciuroParserPipelineTest" --console=plain
./gradlew :core-classifier:testDebugUnitTest --tests "com.sciuro.core.classifier.orchestrator.SciuroIngestionOrchestratorTest" --console=plain
./gradlew :app:assembleDebug --console=plain
```

## Pre-existing failures (NOT introduced by this phase)
- `RegexExtractorsTest > extractAmount parses RM amounts` fails on `main` (function intentionally returns `null` for `0.0`; assertion expects `0.0`).
- `DefaultEngineTriggerUseCaseTest` (6 tests) fails on `main` with `UnsupportedOperationException` from its own dummy `SciuroDatabase` stub.
- `detekt` reports 2 pre-existing `TooManyFunctions` issues in `app` (`EncryptedSettingsProvider`, `NotificationHelper`); none of this phase's files are flagged.
