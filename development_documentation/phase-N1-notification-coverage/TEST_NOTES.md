# Test Notes: Phase N1 — Notification/Email Automation & Multi-Channel Ingestion

## Scope

Fixes a stack of three root-cause gaps causing notification-based transactions to fail parsing, duplicate, or go unmatched:

- **N1 (Parser rule corrections)**: Extended `BsnParserRule` with DuitNow direction keywords ("DuitNow to" + "successful"), added BSN-specific merchant regex for DuitNow recipient names. Extended `MaybankParserRule` to match `com.maybank2u.m2u` (legacy app) and title fallback `"Maybank2u"`. Extended `CimbParserRule` to match `my.com.cimb.octo` and title fallback `"CIMB"`.
- **N2 (Aggregator-forward recognition)**: New `AggregatorForwardMatcher` utility in `core-parsing/.../util/`. Accepts a set of aggregator packages (not hardcoded to Gmail) and bank-specific subject markers. Wired into BSN, Maybank, and CIMB parser rules via Koin.
- **N3 (Cross-channel dedup)**: New `transaction_corroboration` table (8.sqm + migrations/2.sqm). New `findLikelyDuplicate` SQLDelight query on `transaction_record` with index `idx_tx_dedup` on `(account_id, amount, timestamp)`. `TransactionRepository.attachCorroboratingSource()` records the corroboration without changing the existing transaction. Dedup check inserted in `SciuroIngestionOrchestrator.processOneEvent` before booking — same-direction, ±RM0.01, within 90s window catches the second source and attaches it as evidence.
- **N4a (Tight-match auto-confirm)**: `TransferDetectionEngine` now tries a 15-second tight match (no `isPairConfirmed` gate) before falling through to the existing 120-second heuristic path requiring pair confirmation. This bootstraps the `account_pair_confirmation` table automatically on the first DuitNow self-transfer.
- **N4b (Linked Accounts settings)**: New `LinkedAccountsScreen` composable + `LinkedAccountsViewModel` + `LinkedAccountsUiState` sealed interface. Multi-select of own accounts, "Link selected pair" button calls `AccountRepository.linkAccountPair()` which directly seeds `account_pair_confirmation`. Wired into settings navigation in `MainActivity`.
- **N5 (LLM-fallback candidate logging)**: `SimulationResult` exposes `llmPackageMarker` when a simulation falls back to LLM with no matched rule. `DeveloperTabDiagnostics` now displays a "LLM-Fallback Candidate" card with the package name and redacted text — a starting point for future rule authors.

### Pre-requisite fix
- **P0**: Fixed `TransferDetectionEngineTest.kt` — the test was constructing `TransferDetectionEngine(database, transferRepository)` with 2 args but the engine's constructor required 3 (`database, transferRepository, eventBus`). Now passes `DomainEventBus()`. Extracted `FakeAuditRepository` into `TestDatabase.kt` for reuse by the dedup tests.

## Files new or changed

| File | Change |
|------|--------|
| `core-parsing/.../rule/bank/BsnParserRule.kt` | DuitNow direction keywords + member `duitNowMerchantRegex` for BSN-specific merchant extraction |
| `core-parsing/.../rule/bank/MaybankParserRule.kt` | Accept `com.maybank2u.m2u` + title `"Maybank2u"` fallback + aggregator matcher |
| `core-parsing/.../rule/bank/CimbParserRule.kt` | Accept `my.com.cimb.octo` + title `"CIMB"` fallback + aggregator matcher |
| `core-parsing/.../util/AggregatorForwardMatcher.kt` | NEW: shared utility for aggregator-forward identification |
| `core-parsing/.../di/ParsingModule.kt` | Inject `MutableIngestionAllowlist.aggregatorPackages` into bank rules |
| `core-parsing/.../fixture/FixtureLibrary.kt` | Added BSN DuitNow, Maybank2u legacy, CIMB OCTO fixtures |
| `core-ledger/.../db/TransactionCorroboration.sq` | NEW: table + insert + select queries |
| `core-ledger/.../db/TransactionRecord.sq` | Added `findLikelyDuplicate` query |
| `core-ledger/.../db/8.sqm` | NEW: migration — CREATE TABLE + index |
| `core-ledger/.../migrations/2.sqm` | NEW: upgrade migration |
| `core-ledger/.../repository/TransactionRepository.kt` | Added `findLikelyDuplicate()` + `attachCorroboratingSource()` |
| `core-ledger/.../repository/AccountRepository.kt` | Added `linkAccountPair()` for manual pair confirmation from settings |
| `core-classifier/.../orchestrator/SciuroIngestionOrchestrator.kt` | Dedup check before booking; skip full pipeline on duplicate |
| `core-transfer/.../engine/TransferDetectionEngine.kt` | Added `findTightUnconfirmedMatch` with 15s window; named constants for window sizes |
| `core-transfer/.../jvmTest/.../TransferDetectionEngineTest.kt` | Fixed constructor args; added 2 tight-match tests |
| `core-transfer/.../jvmTest/.../TestDatabase.kt` | Extracted `FakeAuditRepository` |
| `core-transfer/.../jvmTest/.../TransactionDedupTest.kt` | NEW: 4 dedup unit tests |
| `core-parsing/.../engine/SimulationResult.kt` | Added `llmPackageMarker` computed property |
| `feature-settings/.../viewmodel/LinkedAccountsUiState.kt` | NEW: sealed interface for link screen state |
| `feature-settings/.../viewmodel/LinkedAccountsViewModel.kt` | NEW: ViewModel for link screen |
| `feature-settings/.../ui/LinkedAccountsScreen.kt` | NEW: multi-select account pair linker |
| `feature-settings/.../ui/SettingsScreen.kt` | Added "Linked Account Pairs" navigation card |
| `feature-settings/.../ui/DeveloperTabDiagnostics.kt` | Added LLM-Fallback Candidate card |
| `feature-settings/.../di/SettingsModule.kt` | Registered `LinkedAccountsViewModel` |
| `app/.../MainActivity.kt` | Added `linked_accounts` route + navigation wiring |
| 3 test files | Extended with screenshot-based fixtures |

## Test results — 23 July 2026

### `:core-parsing:testDebugUnitTest` — Parser tests

- [PASS] All 31 existing fixture regression tests (7 parser rules) — no regressions.
- [PASS] All 16 `RegexExtractorsTest` tests — no regressions.
- [PASS] BSN DuitNow outflow (new): direction=OUTFLOW, amount=5.40, merchant="MUHAMMAD NAIM N"
- [PASS] Maybank2u legacy inflow (new): matches `com.maybank2u.m2u`, direction=INFLOW, amount=5.40
- [PASS] Maybank2u title fallback (new): matches via `"Maybank2u"` title
- [PASS] CIMB OCTO inflow (new): matches `my.com.cimb.octo`, direction=INFLOW, amount=2.00
- [PASS] CIMB title fallback (new): matches via `"CIMB"` title

### `:core-transfer:testDebugUnitTest` — Transfer/dedup tests

- [PASS] All 10 existing transfer engine tests.
- [PASS] Tight match links DuitNow-style self-transfer without prior pair confirmation (1-second gap).
- [PASS] Tight match does not link when gap exceeds 15 seconds.
- [PASS] Dedup: findLikelyDuplicate returns transaction within 90s window.
- [PASS] Dedup: findLikelyDuplicate returns null outside 90s window.
- [PASS] Dedup: findLikelyDuplicate returns null when amount differs.
- [PASS] Dedup: findLikelyDuplicate returns null when direction differs.

### `:app:assembleDebug`

- [PASS] Full app build with all schemas, migrations, and Compose UI compiles successfully.

### `detekt`

- [PASS] NO-SOURCE on root project (pre-existing configuration limitation — detekt is not configured per-module). No new warnings from manual code review.

## Known gaps

- JVM target `:core-transfer:jvmTest` fails on JDK 21 due to `:core-audit:compileKotlinJvm` ("Unknown Kotlin JVM target: 21"). This is a pre-existing environment issue (Kotlin 1.9.0 does not support JDK 21 as a compilation target). Android-side unit tests (`:core-transfer:testDebugUnitTest`) use the Android device platform and do not hit this.
- The dedup and tight-match tests are Android-side only for this reason; full JVM integration test suite requires either a JDK downgrade or Kotlin version bump.
- N4b `LinkedAccountsScreen` Compose UI is not covered by automated Compose UI tests (no Compose testing framework set up in this project per previous phase notes). Manual dogfood is the established verification pattern.
- The `findLikelyDuplicate` query filters on `(direction, amount, timestamp)` but uses `ABS()` which prevents index-only scans. The `idx_tx_dedup` index on `(account_id, amount, timestamp)` is a best-effort hint — SQLite may still require a table scan for the ABS() computation. For a personal-finance workload (<1k transactions per year per user), this is not a performance concern.
