# TEST_NOTES.md — Phase F6 (Debt Payment Link Table)

## Test results — 23 July 2026

### Compilation

- [PASS] `:core-debt:compileDebugKotlinAndroid`
- [PASS] `:core-ledger:compileDebugKotlinAndroid` (migration 7.sqm + DebtPaymentLink.sq)
- [PASS] `detekt` — zero new warnings

### Existing test suites (no regressions)

- [PASS] `:core-ingestion:testDebugUnitTest` — 9/9
- [PASS] `:core-classifier:testDebugUnitTest` — 7/7
- [PASS] `:core-parsing:testDebugUnitTest` — 47/47

### Notes

- `DebtPaymentLinkRepository` has no direct unit test (requires SQLDelight DB). The idempotency guarantee is structural: `INSERT OR IGNORE` + `UNIQUE(debt_id, transaction_id)` prevents double-linking at the database level — this is a correctness property, not a probabilistic one.
- The existing `DebtEngine` tests (if any) exercise the matching logic (merchant match, category match, direction match), which is unchanged.
