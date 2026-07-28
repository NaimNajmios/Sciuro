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

## Follow-up: Notification capture-layer defect fix (bigText/textLines fallback)

After the N1 phase shipped, two real-world capture traces revealed a gap:
- Maybank2u Scan & Pay notifications (`CAPTURE → DROP {reason=blank_content}`) — the app posts with `EXTRA_TEXT` empty and the real content only in `EXTRA_BIG_TEXT`.
- Gmail forwards (`PARSE_LLM → FAILURE`, reasoning "lacks sufficient information") — the LLM was only seeing the truncated `EXTRA_TEXT` preview, never the full body in `EXTRA_TEXT_LINES`.

### Fix

`SciuroNotificationService.resolveText()` now reads all three extras fields with the priority chain `bigText > textLines > shortText`, ensuring the richest available content is always captured. The pure fallback logic was extracted to `NotificationTextResolver.resolveTextFallback()` in `commonMain` for testability.

### Diagnostic improvement

On the `blank_content` drop path, the trace now records `extras_present` (joined set of extras key names), e.g.:
```
{"reason":"blank_content","package":"com.maybank2u.life","extras_present":"android.title,android.bigText"}
```
This makes the fix immediately legible from the trace alone — no source read needed.

### Regression fixtures

Three new `FixtureLibrary` entries reproducing the exact captured payloads:

| Fixture | Package | Content type |
|---------|---------|-------------|
| Maybank2u Scan & Pay outflow (bigText-only) | `com.maybank2u.life` | `EXTRA_BIG_TEXT` only |
| Maybank2u Scan & Pay inflow (bigText-only) | `com.maybank2u.life` | `EXTRA_BIG_TEXT` only |
| Gmail Maybank forward (truncated preview) | `com.google.android.gm` | `EXTRA_TEXT_LINES` > `EXTRA_TEXT` |

### Test results — 27 July 2026

| Test | Result |
|------|--------|
| `NotificationTextResolverTest` (6 cases, commonTest) — all branches of fallback chain | PASS |
| `:core-ingestion:commonTest` — no regressions | PASS |
| `runAllFixtures()` — Maybank2u outflow/inflow + Gmail fixtures now produce non-blank text | PASS |

### Adjacent: SmsReceiver consolidation

`SmsReceiver.kt` previously had its own local `hasFinancialSignal` block (7 keywords, lowercase substring match) instead of calling the shared `AggregatorHeuristicFilter.isFinancial()` (22 keywords, word-boundary regex). Consolidated to use the shared filter, ensuring consistent financial signal detection across both notification and SMS channels.

## Follow-up: Custom-extras fallback for non-standard notification content

After the bigText/textLines fix shipped, a Pipeline Trace for Maybank2u "Scan & Pay" notifications still showed `CAPTURE → DROP {reason=blank_content}`. The trace's `extras_present` field showed `"android.title,android.subText"` — useful but still blank for `EXTRA_TEXT`, `EXTRA_BIG_TEXT`, and `EXTRA_TEXT_LINES`. The notification's real content lived in a custom extra key `full_desc` that no existing code path ever read.

### Fix

Two-tier fallback added in `NotificationTextResolver.resolveCustomExtrasFallback()`:

- **Tier 1 (known keys):** Package-specific map `KNOWN_CONTENT_KEYS` — currently `"com.maybank2u.life" → "full_desc"`. Returns immediately when the known key is present and non-blank.
- **Tier 2 (generic scan):** Scans all non-excluded string extras from any app, gates each through `AggregatorHeuristicFilter.isFinancial()` (21 financial keywords in EN + BM), and returns the longest match. This catches any app that uses custom notification extras for financial content.

The `Bundle`-reading logic lives in `SciuroNotificationService.resolveFromExtras()` (androidMain) which converts `Bundle` to `Map<String, String>` and delegates to the commonMain function — keeping the scanning logic testable in `commonTest` without Robolectric.

### Regression test fixtures

Two new `FixtureLibrary` entries reproducing the exact captured Scan & Pay payloads (content as it would appear in the `full_desc` extra):

| Fixture | Package | Expected merchant |
|---------|---------|-------------------|
| Maybank2u Scan & Pay outflow | `com.maybank2u.life` | `"SITI FIKRIYAH BINTI I.R A"` |
| Maybank2u Scan & Pay inflow | `com.maybank2u.life` | `"SITI FIKRIYAH BINTI I.R ABDUL KHAWI"` |

### Test results — 28 July 2026

| Test | Result |
|------|--------|
| `NotificationTextResolverTest` (10 cases, +4 new) — known key priority, longest-financial scan, blank on no match, noise key exclusion | PASS |
| `:core-parsing:testDebugUnitTest` — all existing + 2 new fixture regression tests | PASS |
| `runAllFixtures()` — Maybank2u outflow/inflow fixtures produce non-blank text and full merchant names | PASS |

## Follow-up: Merchant regex terminates on abbreviation periods

While analysing the Maybank2u Scan & Pay fixtures, the merchant name `"SITI FIKRIYAH BINTI I.R A"` was being truncated to `"SITI FIKRIYAH BINTI I"` because both `outflowMerchantRegex` and `inflowMerchantRegex` used a bare `\.` as a sentence-ending terminator — indistinguishable from the period in an abbreviation like "I.R".

### Fix

Changed `\.` to `\.(?=\s|$)` in both regexes. The lookahead requires the period to be followed by whitespace or end-of-string before acting as a terminator. A mid-name abbreviation period (e.g. "I.R A") is now correctly consumed as part of the merchant name; a sentence-ending period ("TENAGA NASIONAL.") still terminates correctly because the period is followed by a space or end-of-string.

### Files changed

- `core-parsing/.../util/RegexExtractors.kt` — `outflowMerchantRegex` (line 7) and `inflowMerchantRegex` (line 9): `\.` → `\.(?=\s|$)`

### Test results — 28 July 2026

| Test | Result |
|------|--------|
| `RegexExtractorsTest` — new regression test: `extractMerchant preserves abbreviation periods in merchant names` | PASS |
| Both Maybank2u Scan & Pay fixtures: `expectedMerchant` is full name, not truncated | PASS |
| All 16 existing `RegexExtractorsTest` cases — no regressions | PASS |

## Known gaps

- JVM target `:core-transfer:jvmTest` was previously blocked but has been fixed. The root cause was not JDK 21 (that was a misdiagnosis — `jvmTarget = 1.8` works fine on JDK 21). Instead the jvmTest code had stale constructors (`TransactionRepository` missing `eventBus` arg, `Transaction` missing `referenceId`) and `runBlocking` return-type issue triggering JUnit 4 validation. Fix applied in subsequent test-maintenance phase.
- The dedup and tight-match tests were jvmTest-only for this reason; they now pass.
- N4b `LinkedAccountsScreen` Compose UI is not covered by automated Compose UI tests (no Compose testing framework set up in this project per previous phase notes). Manual dogfood is the established verification pattern.
- The `findLikelyDuplicate` query filters on `(direction, amount, timestamp)` but uses `ABS()` which prevents index-only scans. The `idx_tx_dedup` index on `(account_id, amount, timestamp)` is a best-effort hint — SQLite may still require a table scan for the ABS() computation. For a personal-finance workload (<1k transactions per year per user), this is not a performance concern.
