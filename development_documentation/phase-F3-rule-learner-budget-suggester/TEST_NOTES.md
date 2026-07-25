# TEST_NOTES.md — Phase F3 (Rule Learner, CategoryResolver, BudgetLimitSuggester)

## Test results — 23 July 2026

### `:core-classifier:testDebugUnitTest` — CategoryResolverTest (7 tests)

- [PASS] `guessFromStaticHeuristic returns cat_dining for restaurant merchants`
  - Verified Starbucks, McDonalds, KFC, Burger King, Tealive, Warung Pak Ali all resolve to `cat_dining`.
- [PASS] `guessFromStaticHeuristic returns cat_groceries for grocery merchants`
  - Verified Jaya Grocer, Speedmart, Mydin all resolve to `cat_groceries`.
- [PASS] `guessFromStaticHeuristic returns cat_transport for Grab`
  - Verified Grab, GrabPay, GRAB FOOD all resolve to `cat_transport`.
- [PASS] `guessFromStaticHeuristic returns cat_utilities for TNB`
  - Verified Tenaga Nasional resolves to `cat_utilities`.
- [PASS] `guessFromStaticHeuristic is case insensitive`
  - Mixed-case variations of "Starbucks" all resolve correctly.
- [PASS] `guessFromStaticHeuristic returns null for unknown merchant`
  - Unknown vendor and empty string both return null.
- [PASS] `guessFromStaticHeuristic returns null for null-merchant-like strings`
  - AirAsia, Netflix — not in any static list — return null.

### `:core-ingestion:testDebugUnitTest` — MutableIngestionAllowlistTest (9 tests)

- [PASS] All 9 existing tests — no regressions.

### `:core-parsing:testDebugUnitTest` — Parser fixture + regex tests (47 tests)

- [PASS] All 47 existing tests — no regressions.

### `:core-budget:jvmTest` — BudgetLimitSuggesterTest (8 tests)

- [PASS] `suggestLimit returns null when fewer than 3 transactions`
  - Inserted 2 transactions, result is null.
- [PASS] `suggestLimit returns null when exactly 3 transactions`
  - Suggester requires > 3 data points for trimmed mean. Exactly 3 returns null.
- [PASS] `suggestLimit returns trimmed mean for normal distribution`
  - 7 transactions (100–400), result falls within 150–350 range.
- [PASS] `suggestLimit handles all identical amounts`
  - 5 transactions at RM 200, result is exactly 200.0.
- [PASS] `suggestLimit trims outliers correctly`
  - 10 transactions (10–90 + outlier 1000), result < 200 confirming outlier trimmed.
- [PASS] `suggestLimit filters old transactions outside lookback window`
  - Old transaction (91 days ago) at RM 1000 excluded; 5 recent RM 50 transactions yield result < 200.
- [PASS] `suggestAndPublish publishes BudgetLimitSuggested event`
  - 5 transactions inserted, `suggestAndPublish` returns non-null, event bus receives `BudgetLimitSuggested` with matching categoryId and amount.
- [PASS] `suggestAndPublish returns null when insufficient data`
  - 1 transaction only, returns null, no event published.

### detekt

- [PASS] Zero new warnings.

### Full-project compilation

- [PASS] All 22 modules compile with the new database tables, engine classes, and UI.

## Known gaps

- `RuleLearner` has no direct unit test — requires a JVM SQLDelight driver to test the event→DB persistence flow. The subscription pattern (scope.launch → eventBus.events.collect → DB upsert → publish) is structurally identical to the existing `KanbanViewModel` subscription pattern, which is tested via manual dogfood.
- `BudgetLimitSuggester` tests use `System.currentTimeMillis()` for timestamps. Tests pass on JVM but are coupled to real time — a clock abstraction would enable deterministic testing.
