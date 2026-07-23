# Phase F8 — Net Position Rollup

## Summary

- Created `NetPosition` data class in `core-audit` — `totalAccounts`, `totalCash`, `totalInvestments`, `totalDebtsOwed`, `totalDebtsReceivable`, `netWorth`.
- Created `NetPositionSubscriber` in `core-ledger/subscriber/` (moved from `core-audit` due to module layering — needs `SciuroDatabase` which lives in `core-ledger`).
  - Subscribes to `TransactionCategorized`, `TransferMatched`, `CashCredited`, `CashDebited`, `DebtBalanceUpdated`, `InvestmentPriceRefreshed`, `InvestmentTransactionRecorded`.
  - Recomputes `NetPosition` from scratch on each relevant event (pure function over current data, per automation plan §1).
  - Exposes `StateFlow<NetPosition>` for UI consumption.
  - Started in `SciuroApp.onCreate()` alongside the orchestrator and rule learner.
- Registered in `LedgerModule` as a Koin singleton.

## Files changed

| File | Change |
|------|--------|
| `core-audit/.../model/NetPosition.kt` | NEW: NetPosition data class |
| `core-ledger/.../subscriber/NetPositionSubscriber.kt` | NEW: bus subscriber, event-driven recomputation |
| `core-ledger/.../di/LedgerModule.kt` | Registered NetPositionSubscriber |
| `app/.../SciuroApp.kt` | Starts NetPositionSubscriber in onCreate |

## Why it matters

Net Position is the "derive, don't duplicate" proof point. It's never stored — it's always computed fresh from the same append-only facts (accounts, investments, debts) that other modules already maintain. Any module that publishes a relevant event triggers a recomputation automatically. No module needs to know about Net Position — it's a pure consumer of the event bus.
