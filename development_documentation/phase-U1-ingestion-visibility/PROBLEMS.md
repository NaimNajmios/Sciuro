# Phase U1 — User-Facing Ingestion Visibility — PROBLEMS

## Migration 17 — ALTER TABLE adds
`raw_event_staging` gains 4 columns via `ALTER TABLE ADD COLUMN`:
- `activity_status TEXT` (nullable — historical rows are `NULL`, mapped to `DROPPED` by `toActivityLogStatus()` only for rows that have any activity; recent-activity queries filter to `IN ('PARSED','NEEDS_REVIEW','DROPPED')` so `NULL` rows never surface).
- `financial_signal INTEGER NOT NULL DEFAULT 0` — existing captured rows before this migration default to `0` (non-financial). The capture services now write the real signal on new captures; the booking use case treats `0` as non-financial and routes parse failures to `IGNORED` rather than `DROPPED`. Edge case: a row captured before the migration that later gets retried (via developer requeue) will be treated as non-financial. Acceptable for a local finance tracker, but worth recording: legacy `DEAD_LETTER` rows won't produce a banner even if they were financial.
- `activity_reason TEXT` (nullable).
- `user_alerted_at INTEGER` (nullable) — the one-time-banner acknowledgment column. No default, so pre-existing `DROPPED` rows (there are none pre-migration since `DROPPED` is new) need no backfill.

## SQLDelight scalar query gotcha
`selectLastCapturedAt` (`SELECT MAX(captured_at) AS last_captured_at ...`) generates a **row** type, not a scalar. `.asFlow().map { it.last_captured_at }` failed to compile (`it` inferred as `Unit`); the working form is `asFlow().mapToList(...).map { list.firstOrNull()?.last_captured_at }`.

## Naming collision in SQLDelight generated params
`markActivityStatus` uses positional `?` placeholders against `SET activity_status = ?, activity_reason = ?`; the generated parameter names are `activity_status`/`activity_reason` (not `status`/`reason`). Kotlin call sites must use the generated names.

## Parse failure vs "not a transaction" ambiguity
`SciuroParserPipeline.process()` previously returned `StructuredDraft?` (null for both cases). It now returns `ParseResult` (`Success` / `NotATransaction` / `Failure(reason)`), backed by `LlmFallbackParser.lastVerdict`. This is a breaking change to the pipeline's public surface — `TransactionBookingUseCase` and `SciuroParserPipelineTest` were updated. Any future consumer must branch on the sealed type.

## Orchestrator contract change
`SciuroIngestionOrchestrator.processOneEvent` no longer dead-letters on a `null` booking result; the booking use case owns terminal-state transitions. The orchestrator test's fake booking use case was updated to mirror this. If a future booking use case returns `null` WITHOUT setting a terminal state, the event will be silently dropped from recovery — this is a new invariant to document in the booking interface.
