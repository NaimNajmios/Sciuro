# Phase F6 — Debt Payment Link Table

## Summary

- Added migration `7.sqm` with `debt_payment_link` table: `(id PK, debt_id, transaction_id, amount_applied, applied_at, UNIQUE(debt_id, transaction_id))`.
- Created `DebtPaymentLink.sq` with queries: `insertPaymentLink` (INSERT OR IGNORE), `selectPaymentLinksByDebt`, `selectPaymentLinkByTransaction`, `deletePaymentLinkByTransaction`, `sumPaymentsByDebt`.
- Created `DebtPaymentLinkRepository` wrapping the link table with idempotent link creation, per-transaction check, per-debt sum aggregation, and balance recalculation.
- Rewrote `DebtEngine.processDebtPayments()` to:
  1. Find matching transactions per active debt (same logic as before)
  2. Create `debt_payment_link` rows for each match (skips already-linked via INSERT OR IGNORE + UNIQUE constraint)
  3. Recalculate debt balance from the sum of linked payments (not from the transaction list directly)
  4. Publish `DebtBalanceUpdated` / `DebtFullyPaidOff` as before
- Updated `DebtModule` Koin bindings to inject `DebtPaymentLinkRepository` into `DebtEngine`.

## Why this matters

**Before:** `DebtEngine` rescanned all transactions for each debt, summing `amount`s via filter. If a transaction was edited or deleted, the debt balance would be silently wrong until the next full rescan. Double-counting was possible if `processDebtPayments()` was called twice with the same transaction.

**After:** Each payment link is a distinct row. `UNIQUE(debt_id, transaction_id)` prevents double-counting structurally. `INSERT OR IGNORE` makes re-processing idempotent. `sumPaymentsByDebt` recalculates from the link table, not from the raw transaction list — so balance is always consistent with what's actually linked.

## Files changed

| File | Change |
|------|--------|
| `core-ledger/.../db/7.sqm` | NEW: migration for `debt_payment_link` |
| `core-ledger/.../db/DebtPaymentLink.sq` | NEW: insert/select/delete/sum queries |
| `core-debt/.../repository/DebtPaymentLinkRepository.kt` | NEW: link management + balance recalculation |
| `core-debt/.../engine/DebtEngine.kt` | Rewrote to use link table instead of direct transaction sum |
| `core-debt/.../di/DebtModule.kt` | Registered `DebtPaymentLinkRepository` |
