# Phase R2 — Engine Trigger Persistence & Concurrency Hardening — TEST_NOTES

## Summary
`DefaultEngineTriggerUseCase` previously kept seven per-engine debounce timestamps (`lastTransferRunMs`, `lastBudgetRunMs`, etc.) as `var` fields in memory. They reset to `0L` on process death, so after any crash-restart all 7 engines fired on the first booked event — a thundering-herd of full-table scans. Concurrent events could also both pass the debounce check and invoke the same engine simultaneously.

This phase makes debounce state durable across process death and serializes engine execution so no engine can run concurrently with itself.

## Changes applied

### R2.1 — Durable debounce timestamps
- `core-ledger/.../config/SettingsProvider.kt` — added two interface methods:
  - `getEngineLastRunMs(engineName: String): Long`
  - `setEngineLastRunMs(engineName: String, timestampMs: Long)`
- `app/.../config/EncryptedSettingsProvider.kt` — implemented both, keyed as `engine_last_run_<engineName>` in the existing `EncryptedSharedPreferences` store (`sciuro_secure_settings`). Consistency with the existing settings pattern; no schema change.
- **Effect:** after a crash-restart, each engine's cooldown resumes from the persisted timestamp instead of resetting to zero, so the 7 engines do not re-fire as a herd on the first event.

### R2.2 — Per-engine serialization
- `core-classifier/.../orchestrator/EngineTriggerUseCase.kt` — rewired `DefaultEngineTriggerUseCase`:
  - Replaced the 7 `var lastXxxRunMs` fields with a persisted keyed store.
  - Added a `debounceMutex` (single `Mutex`) guarding the `shouldRun(engineName, now)` decision — check-and-reserve is atomic.
  - Added a per-engine `Mutex` (lazy map keyed by engine name) acquired around each `runEngine(...)` invocation, so two concurrent events can no longer execute the same engine simultaneously.
  - `shouldRun(engineName, now)` persists the reservation timestamp *before* execution, preserving the existing behavior that a failed engine remains debounced.
  - Engine order, names, 15-second strict `>` window, failure isolation, and cancellation rethrow unchanged.
- `core-classifier/.../di/ClassifierModule.kt` — injects `SettingsProvider` into `DefaultEngineTriggerUseCase`.

### R2.3 — Testability
- Made the 7 engine classes `open` so tests can subclass with lightweight no-op implementations: `TransferDetectionEngine`, `ObligationCycleMatcher`, `BudgetEngine`, `DebtEngine`, `InvestmentEngine`, `ObligationDetectionEngine`, `BnplRiskDetector`.
- `core-classifier/src/commonTest/.../DefaultEngineTriggerUseCaseTest.kt` (new) — covers:
  - First `triggerAll` invokes all 7 engines in order with 7 `ENGINE` traces.
  - Immediate second call invokes none.
  - Timestamps persisted per engine.
  - Persisted timestamps survive a fresh use-case instance (process-death simulation).
  - All 7 engines attempted even if some fail (failure isolation preserved).
  - Concurrent `triggerAll` calls produce exactly 7 engine traces (no duplicate invocation).
- Updated `SettingsProvider` test fakes (`MutableIngestionAllowlistTest`, `ObligationDetectionEngineTest`) with the two new method stubs.

### R2.4 — Pre-existing test compile fixes (unrelated, required for the suite to build)
- `SciuroIngestionOrchestratorTest.kt` — `Raw_event_staging` constructor missing the G4-added `is_read` parameter.
- `ReviewTierDeciderTest.kt` — `fakeDb` missing G4-added `domainEventLogQueries` / `domainEventDeliveryQueries` properties.
- `core-classifier/build.gradle.kts` — `commonTest` gains `kotlinx-coroutines-core`; added `kotlinx-coroutines-test` to `gradle/libs.versions.toml`.

## Test results
- [PASS] `:core-classifier:compileDebugKotlinAndroid` — clean compile.
- [PASS] `:app:compileDebugKotlin` — full app compiles with the new `SettingsProvider` methods and DI wiring.
- [INCOMPLETE] `:core-classifier:testDebugUnitTest` — added tests written and compile step exercised, but a full green run was not completed before this phase was cut for documentation; re-run before merge.

## Notes
- Timestamps are not sensitive; `EncryptedSharedPreferences` was chosen for consistency with the existing `SettingsProvider` store rather than for secrecy.
- Debounce remains a gate (not a coalescing flow debounce); `BudgetReconciler`'s own 300 ms flow debounce is unrelated and unchanged.
