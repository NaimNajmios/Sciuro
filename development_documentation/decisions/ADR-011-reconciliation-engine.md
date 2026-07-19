# ADR 011: Balance Reconciliation Engine

## Context
A ledger built entirely on push notification interception will eventually drift. Missed notifications, phone reboots, or incorrect parsing can cause the `Account` balance to mathematically diverge from the sum of its `TransactionRecord`s. The user must be able to declare "My balance is X" and have the system safely realign itself without altering historical transactions.

## Decision
1. **Schema**: Created `cash_adjustment` table in `core-ledger`. This represents a localized fix to force the ledger into alignment.
2. **ReconciliationEngine**: Built in `core-ledger`. It executes the following logic:
   - Sums all historical INFLOW and OUTFLOW transactions for an account.
   - Sums all existing `cash_adjustment` records.
   - Compares this calculated balance against the user's declared balance.
3. **Adjustment**: If a mismatch (`diff > 0.01`) exists, it creates a new `cash_adjustment` for the exact difference and updates the `Account` balance.
4. **Audit**: This operation is fully audited, marking the source as `AuditSource.USER_MANUAL`. The audit log clearly shows the calculated balance vs. declared balance.

## Consequences
- The ledger remains mathematically pure. We never alter or delete historical transactions to "make the math work."
- Users have a reliable escape hatch if ingestion fails, giving them confidence in the app's accuracy.
