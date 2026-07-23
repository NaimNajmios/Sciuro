# Phase F5 — Investment PriceProvider + Valuation Engine

## Summary

- Added migration `6.sqm` adding `unit_type` column to `investment_record` (defaults to 'UNITS').
- Created `PriceProvider` interface in `core-investment` with `getCurrentPricePerUnit()` and `refresh()`.
- Created `ManualPriceProvider` — stores user-set prices per asset type/symbol in `SettingsProvider`/`EncryptedSharedPreferences`.
- Added `getManualPrice`/`setManualPrice` to `SettingsProvider` interface and `EncryptedSettingsProvider`.
- Created `InvestmentValuationEngine` — computes current value as `unitsHeld × currentPrice`, falls back to `averageBuyPrice` when no live price is set. Publishes `InvestmentPriceRefreshed` on refresh.
- Updated `InvestmentEngine` to publish `InvestmentTransactionRecorded` on unit changes and use `unit_type` from the investment record.
- Updated `InvestmentRepository` to include `unit_type` in all insert/update/mapping operations.
- Registered `ManualPriceProvider` as the `PriceProvider` binding and `InvestmentValuationEngine` as a Koin singleton.
- `Investment` model gained `unitType: String = "UNITS"` field.

## Files changed

| File | Change |
|------|--------|
| `core-ledger/.../db/6.sqm` | NEW: migration adding `unit_type` column |
| `core-ledger/.../db/Investment.sq` | Added `unit_type` to CREATE TABLE, insertInvestment, updateInvestment |
| `core-ledger/.../config/SettingsProvider.kt` | Added `getManualPrice`/`setManualPrice` |
| `app/.../config/EncryptedSettingsProvider.kt` | Implemented manual price persistence |
| `core-investment/.../price/PriceProvider.kt` | NEW: `PriceProvider` interface |
| `core-investment/.../price/ManualPriceProvider.kt` | NEW: user-set price via SettingsProvider |
| `core-investment/.../engine/InvestmentValuationEngine.kt` | NEW: computes current value, emits `InvestmentPriceRefreshed` |
| `core-investment/.../engine/InvestmentEngine.kt` | Publishes `InvestmentTransactionRecorded`, uses `unit_type` |
| `core-investment/.../model/Investment.kt` | Added `unitType` field |
| `core-investment/.../repository/InvestmentRepository.kt` | Updated all queries + mapping for `unit_type` |
| `core-investment/.../di/InvestmentModule.kt` | Registered `ManualPriceProvider`, `PriceProvider`, `InvestmentValuationEngine` |

## Known gap

- `WalletScreen` still uses `it.unitsHeld * it.averageBuyPrice` for displayed value — the `InvestmentValuationEngine.getTotalCurrentValue()` method exists but needs a follow-up Compose wiring to replace the static computation with the live one. This is a narrow UI change that doesn't block P6–P8.
