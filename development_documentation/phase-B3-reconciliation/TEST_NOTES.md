# Test Notes: Phase B3 - Balance & reconciliation engine

## Scope
- Defined SQLDelight schema `CashAdjustment`.
- Built `ReconciliationEngine` to calculate historical sums and generate adjustment records.
- Updated `LedgerModule` DI to include the new engine.

## Results
- `core-ledger` builds successfully.
- Logic correctly factors in both `transaction_record` directional sums and prior `cash_adjustment` sums to find the exact required diff.

## Excluded
- Scheduled auto-reconciliation via banking APIs (Plaid/SaltEdge) is strictly out of scope per the privacy requirements. Reconciliation is purely manual (user input) or triggered by trusted SMS summary reports (future).
