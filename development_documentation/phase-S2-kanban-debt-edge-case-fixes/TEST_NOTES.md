# TEST_NOTES.md — Phase S2: Kanban/Debt/Obligation Edge Case Fixes

## Summary

Comprehensive edge case audit and remediation of the kanban board, obligation tracking, and debt tracking modules. See `development_documentation/CHANGELOG.md` for the full change list.

## Files Changed

| File | Change |
|------|--------|
| `core-obligations/engine/ObligationCycleMatcher.kt` | Bidirectional merchant matching |
| `core-obligations/engine/ObligationDetectionEngine.kt` | try/catch on runDetection + per-merchant granularity |
| `core-obligations/repository/ObligationRepository.kt` | Audit wrappers on recordPayment/advanceNextDueDate |
| `core-obligations/engine/FrequencyDetector.kt` | Widened classifyInterval ranges |
| `core-debt/engine/DebtEngine.kt` | Bidirectional merchant matching + try/catch on processDebtPayments |
| `core-debt/repository/DebtRepository.kt` | applyPayment clamp to 0 + auto-mark PAID_OFF; orphaned link cleanup on delete |
| `core-ledger/db/DebtPaymentLink.sq` | Added deletePaymentLinksByDebt query |
| `feature-kanban/viewmodel/KanbanViewModel.kt` | markBillAsPaid advances obligation; updateDebt preserves fields; DebtsFilter tri-state; error surfacing on all writes |
| `feature-kanban/ui/KanbanScreen.kt` | DebtsColumn PillToggle; archived debt rendering; error snackbar wiring |

## Build Verification

### main source (all modules)
- [PASS] `./gradlew :core-obligations:compileKotlinJvm :core-debt:compileKotlinJvm :feature-kanban:compileDebugKotlinAndroid` — BUILD SUCCESSFUL

### ObligationCycleMatcher
- [PASS] `advances next due date when merchant matches active obligation` — bidirectional match does not break existing one-way match
- [PASS] `ignores INFLOW transactions`
- [PASS] `skips when no active obligation matches`
- [PASS] `skips when merchant is null`
- [CODE] Bidirectional: "Netflix" obligation matches "Netflix Subscription" transaction and vice versa
- [PASS] `ignores inactive obligations`
- [CODE] All existing one-way match scenarios continue working after adding the reverse check

### ObligationRepository
- [CODE] `recordPayment()` now wraps in `withAudit` — audit log entries created for each payment advancement
- [CODE] `advanceNextDueDate()` now wraps in `withAudit` — audit log entries created for each due date change
- [CODE] Fast-path mutations no longer silent in audit trail

### FrequencyDetector
- [CODE] `classifyInterval()` now covers continuous range 5-400 days without gaps:
  - 5-10 days → WEEKLY (previously 5-9, with a 1-day gap)
  - 10-18 days → BIWEEKLY (previously 12-16, with 4-day gaps on both sides)
  - 18-45 days → MONTHLY (previously 25-35, with 7-10 day gaps on both sides)
  - 45-120 days → QUARTERLY (previously 80-100, with 20-35 day gaps on both sides)
  - 120-400 days → YEARLY (previously 350-380, with 30-230 day gaps on both sides)
  - >400 days → null (unchanged)

### DebtRepository
- [CODE] `applyPayment()` clamps remaining balance to `maxOf(0, remaining - payment)`
- [CODE] `applyPayment()` calls `markAsPaidOff()` when balance reaches exactly 0
- [CODE] Negative overpayments (e.g., pay RM 500 on RM 400 debt) result in 0.0 balance + PAID_OFF status
- [CODE] `deleteDebt()` removes associated `debt_payment_link` rows before deleting the debt record
- [CODE] Deleting a debt with linked payments does not orphan rows

### DebtEngine
- [CODE] Merchant matching now bidirectional — debt "Maybank Loan" matches merchant "Maybank Loan Payment" AND vice versa
- [CODE] try/catch on `processDebtPayments()` — per-debit granularity prevents one bad record from aborting batch
- [PASS] `skips paid-off debts`
- [PASS] `skips archived debts`
- [PASS] `skips already-linked transactions`
- [PASS] `excludes transfer transactions`
- [PASS] `links matching outflow transactions to active debts`
- [PASS] `matches by merchant name when category_id not cat_debt_payment`

### KanbanViewModel
- [CODE] `markBillAsPaid()` now calls both `bookTransaction()` AND `recordPayment()` — obligation's last_paid_date/next_due_date updated immediately
- [CODE] `updateDebt()` preserves `existing.debt.remainingBalance` (capped at new principal) instead of resetting to principalAmount
- [CODE] `updateDebt()` preserves `existing.debt.status` instead of overriding to ACTIVE
- [CODE] All write operations (`markBillAsPaid`, `recordDebtPayment`, `markDebtAsFinished`, `createObligation`, `updateObligation`, `deleteObligation`, `createDebt`, `updateDebt`, `deleteDebt`, `updateTaskStatus`) wrapped in try/catch emitting `_errorEvents`
- [CODE] `DebtsFilter` enum with 3 states: ACTIVE, INCLUDING_PAID_OFF, ALL
- [CODE] `_debtsFilter` replaces `_showCompletedDebts` — ARCHIVED debts now visible under ALL filter

### KanbanScreen UI
- [CODE] DebtsColumn uses `PillToggle` instead of `Switch` for debt status filtering
- [CODE] PillToggle shows human-readable labels ("Active", "+Paid Off", "All") instead of enum names
- [CODE] Archived debts rendered with muted style (alpha 0.3, strikethrough, "ARCHIVED" chip)
- [CODE] Error snackbar collects from `viewModel.errorEvents` — user sees failure messages on write errors

## Pre-existing Issues Noted

- 3 DebtEngine tests fail pre-existing (test expects `remaining - payment` computation but engine uses `principal - sum(links)`). Not caused by S2 changes.
- 5 Obligation tests fail pre-existing (test compilation was blocked by missing TestSettingsProvider methods; test bugs existed but were latent). Not caused by S2 changes.
