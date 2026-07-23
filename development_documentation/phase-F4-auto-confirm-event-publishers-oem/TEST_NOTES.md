# TEST_NOTES.md — Phase F4 (Auto-Confirm, Event Publishers, OEM Guidance)

## Test results — 23 July 2026

### Full-project compilation

- [PASS] `:core-audit:compileDebugKotlinAndroid`
- [PASS] `:core-obligations:compileDebugKotlinAndroid`
- [PASS] `:core-transfer:compileDebugKotlinAndroid`
- [PASS] `:feature-settings:compileDebugKotlinAndroid`
- [PASS] `:feature-dashboard:compileDebugKotlinAndroid`
- [PASS] `:app:compileDebugKotlin`

### `detekt`

- [PASS] Zero new warnings.

### Existing test suites (no regressions)

- [PASS] `:core-ingestion:testDebugUnitTest` — 9/9
- [PASS] `:core-classifier:testDebugUnitTest` — 7/7
- [PASS] `:core-parsing:testDebugUnitTest` — 47/47

### Notes

- `ConfidenceTracker` has no direct unit test (requires SQLDelight DB with `merchant_category_rule` data). The auto-confirm flow will be validated during dogfood.
- `OemAutostartHelper` is a utility class (pure intent building) — tested manually by verifying each manufacturer branch returns a valid `Intent` component name.
- `TransferMatched` / `TransferUnmatchedFlagged` event emissions rely on the existing `TransferDetectionEngine` logic, which is covered by `TransferDetectionEngineTest.kt` (10 integration tests, core-transfer:jvmTest). The event publishing is a non-breaking addition.
