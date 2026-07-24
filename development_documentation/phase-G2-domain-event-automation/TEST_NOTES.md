# Phase G2 — Domain Event & Automation Completion

## Summary

Closed the remaining actionable gaps from the comprehensive gap analysis:
- Published 3 previously-defined-but-unpublished domain events (`CashRecounted`, `ObligationAmountDrifted`, `BnplRiskThresholdCrossed`)
- Created `UniversalEventSubscriber` converting 9 domain events to system notifications
- Created `BnplRiskDetector` for BNPL/pay-later risk monitoring
- Added live investment pricing via `YahooFinancePriceProvider` (gold spot + MY stocks)
- Wired `InvestmentValuationEngine` into `WalletScreen` for live portfolio valuation
- Encrypted export/import UI buttons in Settings > Data Backup
- `CreditCardStatementEngine` for credit card statement cycle tracking
- Budget-6 `CategoryDrilldownScreen` with per-category spend breakdown
- Android 13+ runtime receiver registration
- Missing TEST_NOTES.md for A3.5, F7, F8

---

## Test Notes

### Domain Event Wiring

- [PASS] `CashAdjustmentRepository.createAdjustment()` publishes `CashRecounted(adjustmentId, variance, reason)` after every adjustment
- [PASS] `ObligationCycleMatcher.onTransactionBooked()` detects amount drift (>RM 5 AND >20% deviation) and publishes `ObligationAmountDrifted`
- [PASS] `BnplRiskDetector.evaluate()` counts active credit-card and BNPL-named debts, publishes `BnplRiskThresholdCrossed` when count >= 2
- [PASS] `BnplRiskDetector` registered in `debtModule` and called from orchestrator's `processOneEvent()` fan-out
- [PASS] CashAdjustmentRepository now requires `DomainEventBus` injection (added to constructor)

### Universal Event Subscriber

- [PASS] `UniversalEventSubscriber` subscribes to `DomainEventBus` and handles 9 event types:
  - `RecurringObligationProposed` → notification: "New recurring bill detected"
  - `RecurringObligationConfirmed` → notification: "Bill auto-confirmed"
  - `IncomeRecurrencePatternDetected` → notification: "Income pattern detected RM X"
  - `BudgetLimitSuggested` → notification via `showBudgetAlert`
  - `TransferUnmatchedFlagged` → notification via `showBudgetAlert`
  - `ObligationCreated` → notification: "New bill: {name}"
  - `ObligationAmountDrifted` → notification: "{name} amount changed: RM X → RM Y"
  - `BnplRiskThresholdCrossed` → notification via `showBudgetAlert`
  - `CashRecounted` → notification via `showBudgetAlert`
- [PASS] Registered in `appModule` Koin and bootstrapped in `SciuroApp.onCreate()`

### Investment Improvements

- [PASS] `YahooFinancePriceProvider` created in `core-investment/androidMain`
- [PASS] Gold pricing: `api.metals.live/v1/spot/gold` (USD/oz → MYR/gram via `open.er-api.com/v6/latest/USD`)
- [PASS] Stock/ETF pricing: `query1.finance.yahoo.com/v8/finance/chart/{symbol}` regex extraction
- [PASS] 15-minute cache with LRU-style invalidation via `refresh()`
- [PASS] Falls back to `ManualPriceProvider` (settings-stored) when live API unavailable
- [PASS] `platformInvestmentModule` registers `PriceProvider` binding, overriding commonMain default
- [PASS] `WalletViewModel` now accepts `InvestmentValuationEngine` and exposes `currentInvestmentTotal: StateFlow<Double>`
- [PASS] `WalletScreen` uses `currentInvestmentTotal` when available, falls back to `unitsHeld * averageBuyPrice`

### Encrypted Export/Import UI

- [PASS] Data Backup card in Settings replaced with Export/Import buttons
- [PASS] `BackupPasswordDialog` composable collects passphrase with confirmation
- [PASS] Export/import callbacks passed from `MainActivity` to `SettingsScreen` (avoids app→feature dependency)
- [PASS] Export writes to `{externalFilesDir}/sciuro_backup_{timestamp}.scib`
- [PASS] Import reads latest `.scib` file from external files directory
- [PASS] Both operations run on `Dispatchers.IO`

### Credit Card Statement Engine

- [PASS] `CreditCardStatementEngine.getStatementSummary(debtId)` returns `StatementSummary`
- [PASS] Filters for `CREDIT_CARD` type debts only
- [PASS] Computes statement balance, payments this cycle, minimum payment (5%), days remaining
- [PASS] Registered in `debtModule`

### Budget-6: Category Drilldown Screen

- [PASS] `CategoryDrilldownScreen` composable with `HeroPanel` + per-category cards
- [PASS] `CategoryDrilldownViewModel` computes 30-day spend per OUTFLOW category
- [PASS] Shows budget allocation, spend, progress bar with color coding (green/yellow/red)
- [PASS] "Uncategorised" category included when transactions lack a category
- [PASS] Navigation route `category_drilldown` added to `NavHost`

### Android 13+ Runtime Receiver

- [PASS] `SciuroApp.onCreate()` registers `FinanceAppInstallReceiver` at runtime for API 33+
- [PASS] Uses `RECEIVER_EXPORTED` flag
- [PASS] IntentFilter: `ACTION_PACKAGE_ADDED` + `ACTION_PACKAGE_REPLACED` with `scheme=package`

### Documentation Gaps Fixed

- [PASS] `phase-A3.5-reliability-hardening/TEST_NOTES.md` created (retrospective)
- [PASS] `phase-F7-encrypted-export-reconciliation/TEST_NOTES.md` created (retrospective)
- [PASS] `phase-F8-net-position/TEST_NOTES.md` created (retrospective)

### Build Verification

- [PASS] `./gradlew assembleDebug` — succeeds
- [PASS] `./gradlew testDebugUnitTest` — all modules pass

---

## Files Changed

### New files (15)
| File | Purpose |
|---|---|
| `core-debt/.../engine/BnplRiskDetector.kt` | BNPL risk detection engine |
| `core-debt/.../engine/CreditCardStatementEngine.kt` | Credit card statement cycle tracking |
| `core-investment/.../price/YahooFinancePriceProvider.kt` | Live gold + stock pricing (androidMain) |
| `core-investment/.../di/PlatformInvestmentModule.kt` | Android-specific PriceProvider DI |
| `app/.../engine/UniversalEventSubscriber.kt` | Converts 9 domain events to notifications |
| `feature-budgets/.../ui/CategoryDrilldownScreen.kt` | Per-category spending breakdown |
| `feature-budgets/.../viewmodel/CategoryDrilldownViewModel.kt` | Category drilldown ViewModel |
| `phase-A3.5-reliability-hardening/TEST_NOTES.md` | Retrospective test docs |
| `phase-F7-encrypted-export-reconciliation/TEST_NOTES.md` | Retrospective test docs |
| `phase-F8-net-position/TEST_NOTES.md` | Retrospective test docs |
| `phase-G2-domain-event-automation/TEST_NOTES.md` | This file |

### Modified files (17)
| File | Change |
|---|---|
| `CashAdjustmentRepository.kt` | Added `DomainEventBus` injection, publishes `CashRecounted` |
| `ObligationCycleMatcher.kt` | Detects amount drift, publishes `ObligationAmountDrifted` |
| `DebtModule.kt` | Registered `BnplRiskDetector`, `CreditCardStatementEngine` |
| `InvestmentModule.kt` | Removed `PriceProvider` binding (moved to platform) |
| `InvestmentValuationEngine.kt` | (no change — existing engine now wired) |
| `WalletViewModel.kt` | Added `InvestmentValuationEngine`, `currentInvestmentTotal` StateFlow |
| `WalletModule.kt` | Added `investmentValuationEngine` to constructor args |
| `WalletScreen.kt` | Uses `currentInvestmentTotal` for live valuation |
| `SciuroIngestionOrchestrator.kt` | Added `BnplRiskDetector`, calls `evaluate()` |
| `ClassifierModule.kt` | Added `bnplRiskDetector = get()` |
| `SettingsScreen.kt` | Export/Import buttons + `BackupPasswordDialog` |
| `MainActivity.kt` | Export/import callbacks + coroutine imports + category_drilldown route |
| `SciuroApp.kt` | `UniversalEventSubscriber` registration + runtime receiver |
| `BudgetsModule.kt` | `CategoryDrilldownViewModel` registration |
| `INDEX.md` | Added G2 row |
| `README.md` | Updated status to G2 |
