# TEST_NOTES.md — Phase F5 (Investment PriceProvider + Valuation Engine)

## Test results — 23 July 2026

### Full-project compilation

- [PASS] `:core-investment:compileDebugKotlinAndroid`
- [PASS] `:core-ledger:compileDebugKotlinAndroid` (migration 6.sqm + updated Investment.sq)
- [PASS] `:feature-wallet:compileDebugKotlinAndroid`
- [PASS] `:app:compileDebugKotlin`
- [PASS] `detekt` — zero new warnings

### Existing test suites (no regressions)

- [PASS] `:core-ingestion:testDebugUnitTest` — 9/9
- [PASS] `:core-classifier:testDebugUnitTest` — 7/7
- [PASS] `:core-parsing:testDebugUnitTest` — 47/47

### Notes

- `InvestmentValuationEngine` has no direct unit test — requires SQLDelight DB with investment data and a mock PriceProvider. The valuation math (`unitsHeld × currentPrice`, with fallback to `averageBuyPrice`) will be validated during dogfood.
- `ManualPriceProvider` stores prices as strings in EncryptedSharedPreferences — minimal risk, the getter/setter is a simple string→double conversion.
- The `unit_type` migration (`6.sqm`) is a single `ALTER TABLE ... ADD COLUMN ... DEFAULT 'UNITS'` — safe, no data loss.
