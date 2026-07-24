# Phase H0 — Structural Defect Remediation — TEST_NOTES

## Summary
Foundation phase fixing 9 structural defects discovered during pipeline audit.
All changes are internal — no user-facing behavior changes (except the new Resend button on dead-letter cards).

## Changes applied

### H0.1 — Feed RuleLearner (dead learning loop)
- Injected `DomainEventBus` into `TransactionRepository` (`TransactionRepository.kt:17-21`)
- `reviewTransaction` now publishes `TransactionRecategorized` when category changes (`:131-140`)
- `approveTransaction` now publishes `TransactionCategorized` when category is set (`:109-120`)
- `editTransaction` now publishes `TransactionRecategorized` when category changes (`:202-211`)
- Updated `LedgerModule.kt` to pass 4th constructor arg (`DomainEventBus`)
- **Effect**: user reviews now accumulate `merchant_category_rule` rows, feeding `CategoryResolver.learnedRule` path

### H0.2 — Category integrity (dangling category IDs)
- Remapped `CategoryResolver.guessFromStaticHeuristic` to use existing seeded category IDs:
  - `cat_dining` → `cat_exp_1` (Food & Beverage)
  - `cat_groceries` → `cat_exp_6` (Groceries)
  - `cat_transport` → `cat_exp_2` (Transportation)
  - `cat_utilities` → `cat_exp_3` (Bills)
- Updated `CategoryResolverTest.kt` expected values accordingly
- **Effect**: auto-categorized transactions now reference valid FK targets

### H0.3 — Confidence capping + ConfidenceScorer
- Created `core-parsing/.../util/ConfidenceScorer.kt` (shared additive formula, `coerceAtMost(1.0f)`)
- Replaced 7-way duplicated confidence formula in all parser rules
- **Effect**: confidence never exceeds 1.0; single source of truth for scoring

### H0.4 — Staging drainer (stranded event recovery)
- Added `selectStrandedEvents`, `selectStrandedEventsCount`, `requeueRawEvent` queries to `RawEventStaging.sq`
- Added `getStrandedEvents()`, `countStrandedEvents()`, `requeueRawEvent()` to `RawEventRepository`
- Orchestrator `startListening` now calls `recoverStrandedEvents()` before collecting
- Attempt cap: `MAX_ATTEMPTS = 3` — events exceeding count are dead-lettered
- **Effect**: PENDING events on crash are re-processed on next app start; stale PROCESSING events reset by worker

### H0.5 — Dead-letter resend
- Added `resendDeadLetter(rawEventId)` to `SettingsViewModel`
- Added Resend button with Refresh icon on each dead-letter card in `DeveloperTabIngestionLog`
- **Effect**: README's "resend capability" is now implemented

### H0.6 — Collector resilience (unlimited restart)
- Replaced one-shot restart with `collectEventsWithRetry()`: `while(true)` + exponential backoff (1s → 60s cap)
- **Effect**: pipeline self-heals from any transient collector failure

### H0.7 — Worker dedup + repurpose
- Removed duplicate `IngestionReconciliationWorker` registration from `MainActivity`
- Single registration in `SciuroApp` at 15-min interval with UPDATE policy
- Repurposed worker to reset stale PROCESSING events back to PENDING, count stranded, probe listener binding
- **Effect**: one worker instead of two; stale events recoverable between app restarts

### H0.8 — Housekeeping
- Removed empty `:core-llm` from `settings.gradle.kts:26` and `app/build.gradle.kts:88`
- Catalog-ized `work-runtime-ktx:2.9.0` and `androidx.sqlite:2.4.0` in `libs.versions.toml`
- Deleted stale `sqldelight/migrations/` directory (SQLDelight 1.x leftovers)
- Added N1 phase row to `INDEX.md`
- Fixed README fixture count 31 → 34
- **Effect**: AGENTS.md §19.2 compliance; root directory clean

### H0.9 — Parser engine reconciliation
- `DeterministicParser.parse` now iterates all matching rules, skipping null extracts
- `SimulationEngine.simulate` now matches same semantics (first `matches && extractedDraft != null`)
- **Effect**: both engines produce identical results; simulator no longer diverges from production

## Test results
- [PASS] `:core-parsing:testDebugUnitTest` — all 47 parser tests pass (7 rule suites + regex extractors)
- [PASS] `:core-classifier:testDebugUnitTest` — CategoryResolver tests pass with remapped IDs
- [PASS] `:core-ledger:testDebugUnitTest` — ledger tests pass
- [PASS] `detekt` — zero issues
- [PASS] `:core-parsing:compileDebugKotlinAndroid` — clean compile
- [PASS] `:core-classifier:compileDebugKotlinAndroid` — clean compile
- [PASS] `:core-ledger:compileDebugKotlinAndroid` — clean compile
- [PASS] `:core-ingestion:compileDebugKotlinAndroid` — clean compile
- [PASS] `:core-audit:compileDebugKotlinAndroid` — clean compile
- [PASS] `:app:compileDebugKotlinAndroid` — clean compile (post catalog fix)

## Known pre-existing issues (not addressed)
- `:core-transfer:jvmTest` fails on JDK 21 (Kotlin 1.9.0 limitation)
- `:core-ingestion:testDebugUnitTest` fails — `EmailSourceAdapterTest.kt` references non-existent email adapter
- Feature modules: `PullToRefreshContainer`, `rememberSharedContentState` unresolved (Material3 compose BOM API drift)
