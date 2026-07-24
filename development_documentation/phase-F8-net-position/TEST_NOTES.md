# TEST_NOTES.md — Phase F8: Net Position Rollup

## Retrospective (phase completed prior to G2)

### NetPositionSubscriber
- [PASS] Subscribes to `DomainEventBus` and recomputes net worth on every relevant event
- [PASS] Handles 7 event types: `TransactionCategorized`, `TransactionRecategorized`, `TransferMatched`,
  `CashCredited`, `CashDebited`, `CashRecounted`, `InvestmentTransactionRecorded`
- [PASS] Formula: net position = accounts balance + investment current value - debts remaining balance
- [PASS] Uses `derive, don't duplicate` architecture — computed from live data, not stored as a separate field
- [PASS] Bootstrapped in `SciuroApp.onCreate()` alongside orchestrator and rule learner

### Dashboard Integration
- [PASS] `DashboardViewModel` reads net position via direct computation (not via subscriber)
- [PASS] `DashboardState.netPosition` includes accounts + investments + net debts
- [PASS] Net position displayed as RM figure in Dashboard hero panel
- [PASS] Balance history chart based on daily transaction deltas

### Event Coverage
- [PASS] `TransactionCategorized` — triggers position recalculation
- [PASS] `TransactionRecategorized` — triggers position recalculation
- [PASS] `TransferMatched` — triggers position recalculation
- [PASS] `CashCredited` — triggers position recalculation
- [PASS] `CashDebited` — triggers position recalculation
- [PASS] `CashRecounted` — triggers position recalculation (event published in G2)
- [PASS] `InvestmentTransactionRecorded` — triggers position recalculation

### Build Verification
- [PASS] `assembleDebug` — successful
- [PASS] `testDebugUnitTest` — all modules pass
- [MANUAL] Net position verified by manually summing account balances + investment value - debts
- [MANUAL] Net position updates verified after booking a transaction in the UI
