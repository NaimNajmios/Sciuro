# ADR-026 — Durable Per-Engine Debounce State

- **Status:** Accepted
- **Date:** 2026-08-02
- **Applies to:** `core-classifier` (`DefaultEngineTriggerUseCase`), `core-ledger` (`SettingsProvider`), `app` (`EncryptedSettingsProvider`)

## Context

`DefaultEngineTriggerUseCase` debounces all 7 downstream engines (transfer, obligation_cycle, budget, debt, investment, obligation_detect, bnpl) with a shared 15-second cooldown to avoid redundant full-table scans during burst ingestion. The original implementation kept seven `var lastXxxRunMs = 0L` fields in memory.

Two defects followed from this design:

1. **Thundering herd after process death.** All seven timestamps reset to `0L` on process death. After any crash-restart, the first booked event re-armed all 7 engines simultaneously — the exact full-scan storm the debounce exists to prevent.
2. **Unprotected check-and-reserve.** `triggerAll()` can be entered concurrently (4 live + 8 recovery events per the orchestrator caps). Two concurrent events could both read a stale timestamp, both pass the debounce check, and both invoke the same engine. `TransactionBookingUseCase` protects dedup with a `Mutex`, but engine triggers were unprotected.

No decision means: every crash-restart degrades to a thundering-herd of 7 full-table scans, and concurrent events can duplicate engine work (double settlement, double BNPL events, repeated budget corrections).

## Options Considered

### Option 1 — Persist timestamps + per-engine Mutex (chosen)

Store last-run timestamps in the existing `SettingsProvider` (backed by `EncryptedSharedPreferences`), guard the check-and-reserve with a single debounce `Mutex`, and serialize each engine's execution with a per-engine `Mutex` keyed by engine name.

- Pros: Fixes both defects at the root. Reuses the existing `SettingsProvider` seam already injected into `core-classifier` (no new Koin modules, no new Android-only dependency in `commonMain`). Cheap — two interface methods. Persistence is best-effort and non-sensitive, so a lost write merely restarts a single engine's cooldown.
- Cons: `SettingsProvider` is a wide interface; two more methods ripple to its test fakes. Timestamps survive only as long as the prefs do — a wiped app loses them (acceptable; identical to all other settings).
- Fit: Correct layering — the interface lives in `core-ledger/commonMain`, the Android-backed implementation in `app`.

### Option 2 — Keep timestamps in-memory, add only per-engine Mutex

Fix the concurrency defect but leave timestamps process-local.

- Pros: Minimal diff (one `Mutex`).
- Cons: Leaves the thundering-herd-after-restart defect fully unresolved — the more severe of the two. Rejected.

### Option 3 — New dedicated `EngineRunClock` / store interface

Introduce a fresh abstraction owned by `core-classifier` with its own Android actual implementation.

- Pros: Keeps `SettingsProvider` narrower.
- Cons: New interface + new Android actual + new Koin module + a second encrypted-prefs store duplicating the existing pattern, for a two-method persistence concern. Higher surface area for no functional gain. Rejected.

### Option 4 — Persist to SQLDelight (DB-backed table)

Store last-run timestamps in a database table alongside the ledger.

- Pros: Transactional, survives app-data-clear differently, queryable.
- Cons: Requires a schema migration for a debounce cache that is inherently ephemeral and loss-tolerant. Over-engineering — the data is neither sensitive nor must-be-durable. Rejected.

## Decision

Adopt **Option 1**:

1. Add `getEngineLastRunMs(engineName)` / `setEngineLastRunMs(engineName, timestampMs)` to `SettingsProvider`; implement in `EncryptedSettingsProvider` using `engine_last_run_<engineName>` keys.
2. In `DefaultEngineTriggerUseCase`, replace the 7 `var` fields with a persisted keyed store, guard the `shouldRun(engineName, now)` check-and-reserve with a single `debounceMutex`, and wrap each engine invocation in a per-engine `Mutex` (lazy map keyed by engine name).
3. Persist the reservation timestamp *before* executing the engine, preserving the existing contract that a failed engine stays debounced.
4. Keep the existing 15-second strict `>` window, engine order, names, failure isolation, and cancellation rethrow.

## Consequences

- **Positive:** No crash-restart thundering herd; no concurrent duplicate engine invocation; no new modules or dependencies; consistent with existing settings persistence.
- **Negative:** `SettingsProvider` grows by two methods (all test fakes updated); two `Mutex` layers add a negligible scheduling cost.
- **Trade-off accepted:** The debounce timestamp is best-effort persistence — a prefs write failure loses one engine's cooldown, never corrupts state.
- **Non-goals:** Not a coalescing flow debounce (still a gate). `BudgetReconciler`'s 300 ms flow debounce untouched.
