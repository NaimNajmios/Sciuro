# TEST_NOTES — Core-Classifier Edge-Case Hardening

**Phase:** classifier-edge-cases (Phases 1-4)
**Date:** 2026-07-29

## Tests Executed

### `:core-classifier:testDebugUnitTest` — 24 tests, all passing

| Test | Result | Notes |
|------|--------|-------|
| `SciuroIngestionOrchestratorTest` — 3 tests | ✅ All pass | Updated for periodic `recoverStrandedEvents` and while-loop scheduling. Tests use polling waits (max 5s) instead of fixed `delay(500)` to avoid flakiness. |
| `CategoryResolverTest` | ✅ Pass | Covered by existing test suite |
| `ReviewTierDeciderTest` | ✅ Pass | Covered by existing test suite |

## Changes Verified

### Phase 1 — Quick Wins

| Case | Verification |
|------|------------|
| **2a** Empty package guard | Verified `AccountMatcher` skips `selectAccountByPackage` for blank input |
| **2b** Blank merchant guard | `CategoryResolver.resolve()` returns null for blank merchant; `hasLearnedRule()` returns false |
| **2c** Locale-independent lowercase | All 4 files use `buildString { forEach { append(it.lowercaseChar()) } }` — no `String.lowercase()` calls remain |
| **2f** NaN/Infinity validation | Guard dead-letters before entering mutex |
| **3a** markProcessing in mutex | Moved into `dedupMutex.withLock` block |
| **6b** isHighConfidence centralized | Single extraction, used in 3 locations |

### Phase 2 — Status Guards

| Case | Verification |
|------|------------|
| **2e/3b** Re-processing prevention | SQL: `WHERE status = 'PENDING'` on markProcessing. Kotlin: status check inside mutex. |

### Phase 3 — Account Cleanup

| Case | Verification |
|------|------------|
| **4d** Account deletion cascade | `AccountRepository.deleteAccount()` wraps status update + rule cleanup in `database.transaction { }`. New `deleteMerchantAccountRuleByAccount` query. |

### Phase 4 — Deeper Changes

| Case | Verification |
|------|------------|
| **3c** Dedup merchant criterion | `findLikelyDuplicate` SQL now includes `merchant` with null-safe OR pattern. `TransactionRepository.findLikelyDuplicate()` accepts `merchant: String? = null`. Call site passes `draft.merchant`. |
| **5a** Parser null retry | Parser failure no longer calls `markDeadLetter`. `recoverStrandedEvents` runs every 60s via `while(true)` loop in `startListening`. |
| **5d** Max reconnects | `MAX_RECONNECT_ATTEMPTS = 10` — exits coroutine after 10 consecutive failures. |

## Build

- `./gradlew assembleDebug` — ✅ BUILD SUCCESSFUL
- `./gradlew :core-classifier:testDebugUnitTest` — ✅ 24/24 passing
- `./gradlew :core-ledger:testDebugUnitTest` — ✅ NO-SOURCE (no tests in ledger)
- `./gradlew :core-audit:testDebugUnitTest` — ✅ NO-SOURCE
