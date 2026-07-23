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

### `detekt`

- [PASS] Zero new warnings.

### Full-project compilation

- [PASS] All 22 modules compile with the new database tables, engine classes, and UI.

## Known gaps

- `RuleLearner` has no direct unit test — requires a JVM SQLDelight driver to test the event→DB persistence flow. The subscription pattern (scope.launch → eventBus.events.collect → DB upsert → publish) is structurally identical to the existing `KanbanViewModel` subscription pattern, which is tested via manual dogfood.
- `BudgetLimitSuggester` has no direct unit test — requires transaction data in the database. The trimmed mean algorithm will be validated during dogfood on real data.
