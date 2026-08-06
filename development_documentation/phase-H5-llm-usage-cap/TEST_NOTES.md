# Phase H5 — LLM Usage Cap — TEST_NOTES

## Summary
Added a durable daily LLM call cap and an inter-call throttle to `LlmFallbackParser`. The daily counter is persisted in `EncryptedSharedPreferences` (via the new `LlmUsageStore` implemented by `EncryptedSettingsProvider`), resets at local midnight, and defaults to a configurable limit of 50 calls/day. When the cap is exhausted the parser skips the provider call, traces `daily_cap_exceeded`, and the pipeline routes the deterministic partial draft to the Review Inbox as `reviewTier = UNTRUSTED`. Also added a minimum 2-second gap between provider calls (`lastCallTimestamp` + `minCallIntervalMs`), surfaced daily usage and cache hit rate in the Developer Health tab, and fixed an existing bug where `UNTRUSTED` transactions were marked reviewed and bypassed the Review Inbox.

## Changes applied

### H5.1 — Daily cap counter + configurable limit
- `LlmParsingConfig`: added `dailyLlmCallLimit: Int = 50` and `minCallIntervalMs: Long = 2_000`.
- New `LlmUsageStore` interface in `core-ledger/.../config/LlmUsageStore.kt`:
  - `dailyLlmCallCount()`, `incrementDailyLlmCallCount()`, `dailyLlmCallLimit()`, `setDailyLlmCallLimit(limit)`.
- `EncryptedSettingsProvider` now implements `LlmUsageStore`:
  - Keys: `llm_usage_date` (ISO local date), `llm_usage_count`, `llm_daily_limit` (default 50, clamped 1..1000).
  - Lazy midnight reset: the count reads as 0 when the stored date differs from the current local date; the increment rewrites date+count.
  - `incrementDailyLlmCallCount()` uses synchronous `commit()` so the reservation is durable before the HTTP call.
- `LlmUsageDates.kt`: shared `currentLocalDateKey()` (uses `kotlinx.datetime.Clock.System.todayIn`) and testable `localDateKeyFor(timestampMs, timeZone)`.
- DI: `appModule` registers `single<LlmUsageStore> { get<SettingsProvider>() as LlmUsageStore }`; `ParsingModule` wires `usageStore = getOrNull()` into `LlmFallbackParser`.
- UI: Intelligence Settings now exposes a "Daily LLM Call Limit" numeric field (ViewModel + screen + `settings_llm_daily_limit` string).

### H5.2 — Admission gate in LlmFallbackParser
- New constructor param `usageStore: LlmUsageStore? = null`.
- `admitCall(event)` guarded by a coroutine `Mutex`:
  1. If `used >= limit` → trace `daily_cap_exceeded` (with `daily_count`/`daily_limit`), return false, no provider call.
  2. Wait out `lastCallTimestamp + minCallIntervalMs - now` via `delay()` to enforce the 2-second gap.
  3. `incrementDailyLlmCallCount()` and update `lastCallTimestamp`.
- `lastCallTimestamp` is exposed as a public read-only property.
- Cache hits, missing API key, and open circuit-breaker paths do **not** consume quota and do not advance `lastCallTimestamp`.
- Trace detail now carries `provider_called` (`true` only for real HTTP attempts; `false` for cache_hit, circuit_breaker_open, llm_disabled_or_no_key, daily_cap_exceeded).

### H5.3 — UNTRUSTED → Review Inbox routing fix
- `ReviewTier.isAutoReviewed` added: only `AUTO_SILENT` / `AUTO_UNDO`.
- `TransactionBookingUseCase` now uses `tier.isAutoReviewed` instead of `tier != MANUAL`, so `UNTRUSTED` (and `MANUAL`) transactions are persisted with `is_reviewed = 0` and appear in `selectUnreviewedTransactions` (Dashboard Review Inbox + Kanban).
- Cap-exhaustion flow: pipeline returns the deterministic partial draft with `isUntrustedFallback = true` → booking sets `reviewTier = UNTRUSTED`, `isReviewed = false`.
- **Known limitation (decision):** if deterministic parsing produces no draft at all, the event is still dead-lettered (no amount/direction to book). Routing a null draft into the Review Inbox would require a schema/model change and was intentionally left out of this phase.

### H5.4 — Developer Health metrics
- `PipelineTrace.sq`:
  - `countTraceByOutcomeSince` → `countTraceByOutcomeInRange(start, end)` (date-filtered).
  - New `countParseLlmProviderCallsInRange(start, end)` (`detail_json LIKE '%provider_called=true%'`).
  - New `countParseLlmCacheHitsInRange(start, end)` (`detail_json LIKE '%verdict=cache_hit%'`).
- `PipelineMetrics` extended with `dailyLlmCallCount`, `dailyLlmCallLimit`, `cacheHitRate`.
- `DeveloperSettingsViewModel.loadHealthData()` computes provider calls, cache hits, and cache hit rate (cacheHits / lookups) over the 7-day window; daily count/limit come from `LlmUsageStore`.
- `DeveloperTabHealth` shows:
  - "LLM fallback calls (7d)" → actual provider calls.
  - "LLM calls today (limit N)" → `count / limit`, tinted `signalDanger` when the cap is reached.
  - "Cache hit rate (7d)".
  - "Dead letters (7d)".

## Test results
- [PASS] `LlmFallbackParserTest` — 17/17 (new: cap skip, counter increment, cache-quota non-consumption, no-key non-consumption, midnight-reopen, 2s gap).
- [PASS] `SciuroParserPipelineTest` — 7/7 (new: unavailable LLM → deterministic fallback marked `isUntrustedFallback`).
- [PASS] `ReviewTierTest` (new) — 2/2 (`isAutoReviewed` semantics).
- [PASS] `LlmUsageDatesTest` (new, jvm) — 3/3 (UTC epoch, local-midnight boundary, just-before-midnight).
- [PASS] `PipelineTraceMetricsTest` (new, jvm) — 2/2 (provider vs cache-hit counts, window exclusion).
- [PASS] `core-ledger:jvmTest`, `core-audit:jvmTest`.
- [PASS] `assembleDebug` compile of app + all changed modules.
- [PRE-EXISTING FAILURES, unrelated to this phase] `RegexExtractorsTest.extractAmount parses RM amounts` and `DefaultEngineTriggerUseCaseTest` (6 tests) — confirmed failing on the base commit via `git stash`.

## Design decisions
- Limit is authoritative from `LlmUsageStore` when present; `config.dailyLlmCallLimit` is the fallback for tests/no-store.
- The 2-second gap is measured from call admission (start), not completion.
- Cache hits are intentionally quota-free and gap-free so repeated same-transaction notifications never burn the quota.
- Provider-call metrics use a detail flag rather than a new table column to avoid a schema migration.
