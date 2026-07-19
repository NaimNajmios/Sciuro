# ADR 009: Recurring Obligation Auto-Detection

## Context
Sciuro needs to transition from a passive ledger into an active financial tracker. A key requirement is automatically predicting upcoming bills and subscriptions (obligations) based on historical transaction data, eliminating the need for the user to manually input their recurring expenses.

## Decision
1. **SQLDelight Integration**: Created the `obligation` schema within the central `SciuroDatabase` in `core-ledger`. This ensures strict referential integrity (foreign keys to `Category` and `Account`) without spinning up multiple SQLite database files.
2. **ObligationDetectionEngine**: Created in the `core-obligations` module. This engine scans the `transaction_record` table.
3. **Detection Heuristic**:
   - Groups transactions by merchant name.
   - Identifies instances where 3 or more transactions exist for the same merchant.
   - Checks if the transaction amounts are nearly identical (`delta < 2.0`).
   - If a pattern is confirmed and no existing obligation matches, it creates a new `Obligation` projecting the next due date 30 days from the most recent transaction.
4. **Audit Trail**: Creation of an obligation is routed through the `ObligationRepository` leveraging the `AuditableRepository` base class, with the source marked as `SYSTEM_AUTO`.

## Consequences
- The system can automatically detect subscriptions like Netflix or Spotify and surface them on a dashboard.
- The pattern matching is currently naive (fixed 30 days, exact amount matching). Future iterations will refine this with ML clustering and variable-amount detection.
