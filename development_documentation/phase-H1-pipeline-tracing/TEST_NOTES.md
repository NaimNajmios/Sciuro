# Phase H1 — DB-Backed Pipeline Tracing — TEST_NOTES

## Summary
Replaced all `println`/`printStackTrace` pipeline logging with structured DB-backed trace table (`pipeline_trace`). Every pipeline stage now writes a trace row: capture, parsing (regex + LLM), staging transitions, dedup, categorization, account matching, booking, engine invocations.

## Changes applied

### H1.1 — Trace enums + interface (core-audit)
- `trace/TraceStage.kt` — 10 stages: CAPTURE, STAGING, PARSE_REGEX, PARSE_LLM, DEDUP, CATEGORIZE, ACCOUNT_MATCH, BOOK, ENGINE, EVENT
- `trace/TraceOutcome.kt` — 5 outcomes: SUCCESS, FAILURE, FALLBACK, SKIP, DROP
- `trace/PipelineTracer.kt` — interface: `suspend fun trace(rawEventId, transactionId, stage, outcome, durationMs, confidence, detail)`

### H1.2 — Schema (core-ledger)
- `PipelineTrace.sq` — table definition + 4 queries (insertTrace, selectTraceByEvent, selectTraceByTransaction, selectRecentTrace, deleteTraceOlderThan)
- `10.sqm` — migration: CREATE table + 4 indexes (idx_trace_event, idx_trace_stage_time, idx_audit_entity, idx_staging_status, idx_tx_reviewed, idx_tx_dedup_direction)

### H1.3 — Implementation + wiring
- `trace/SqlDelightPipelineTracer.kt` — SQLDelight-backed implementation
- `LedgerModule.kt` — Koin `single<PipelineTracer>`

### H1.4 — Orchestrator trace points (core-classifier)
- Injected `PipelineTracer` into `SciuroIngestionOrchestrator` (13th constructor param)
- Replaced all 5 `println` + 3 `printStackTrace` calls with `tracer.trace()`
- Added 12 trace points at key decision points: STAGING (PROCESSING transition, attempt-cap dead-letter, processed), PARSE_REGEX/PARSE_LLM (with timing), DEDUP (skip), CATEGORIZE, ACCOUNT_MATCH, BOOK, 7 ENGINE traces
- Updated `ClassifierModule.kt` to pass `tracer = get()`

### H1.5 — LLM fallback trace points (core-parsing)
- Injected optional `PipelineTracer?` into `LlmFallbackParser`
- Replaced all 8 `println` + 1 `printStackTrace` calls
- Added `traceParse` helper with verdict/model/consecutive_failures/breaker_state
- Traces: circuit_breaker_open, llm_disabled_or_no_key, api_error, empty_response, timeout, network_error, malformed_response, unexpected_error, success
- Updated `ParsingModule.kt` to pass `tracer = get()`

### H1.6 — Capture trace points (core-ingestion)
- `SciuroNotificationService`: traces DROP (allowlist_reject, blank_content, non_financial_aggregator) and SUCCESS (with package)
- `SmsReceiver`: traces DROP (allowlist_reject, non_financial_sms) and SUCCESS (with sender)
- Both use `receiverScope.launch` for fire-and-forget trace writes

### H1.7 — Engine trace points (orchestrator-level)
- One ENGINE trace per invocation: transfer, obligation_cycle, budget, debt, investment, obligation_detect, bnpl
- Runs inside `processOneEvent` sequential fan-out — same transaction_id scoped

### H1.9 — Retention purge
- `RawEventRepository.purgeOldTraces(beforeMs)` — delegates to `deleteTraceOlderThan`
- `IngestionReconciliationWorker` calls purge on each run (30-day retention)

### H1.8 — Mutation+audit transaction wrapping (DEFERRED)
- **Attempted**: wrapping `withAudit` in `database.transactionWithResult { }`
- **Reverted**: SQLDelight's `transactionWithResult` lambda is non-suspend in KMP commonMain; cannot call suspend `withAudit` from within it
- **Deferred**: requires either a suspend-aware transaction wrapper or restructuring AuditableRepository

## Test results
- [PASS] `:core-parsing:testDebugUnitTest` — all 47 parser tests pass
- [PASS] `:core-classifier:testDebugUnitTest` — CategoryResolver tests pass
- [PASS] `:core-ledger:testDebugUnitTest` — ledger tests pass
- [PASS] `detekt` — zero issues
- [PASS] All core modules compile cleanly (no warnings, no errors)
- [PASS] SciuroNotificationService + SmsReceiver trace calls in coroutine scope
- [PASS] Migration `10.sqm` — 1 new table + 4 new indexes

## Known limitations
- DomainEventBus publishes are not traced (deferred to Phase H2)
- Engine trace points are stage-level only; don't capture match/no-match verdict detail
- LLM prompt content not stored in trace (available via existing `lastDebugCapture` for diagnostics tab)
- No trace viewer UI (Phase H5)
- Audit+data not in single DB transaction (SQLDelight KMP limitation)
