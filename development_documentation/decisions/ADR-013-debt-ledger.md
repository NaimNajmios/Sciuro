# ADR 013: Debt Ledger Module

## Context
Standard financial ledgers simply track transactions (income and expense). However, a payment to a credit card or a loan is not an expense; it is a transfer of assets that reduces a liability. Sciuro requires a dedicated module to manage these liabilities (debts) separately from standard spending.

## Decision
1. **Module Creation**: Scaffolded an isolated `core-debt` module. This enforces separation of concerns between standard recurring obligations (like subscriptions) and amortizing debt.
2. **Schema**: Added `debt_record` table to `core-ledger` `SciuroDatabase`. This ensures strict foreign key mapping to the central `Account` table if a debt is represented as an account.
3. **Debt Engine**: Created `DebtEngine` that scans standard `TransactionRecord`s. If a transaction is an OUTFLOW categorized as `cat_debt_payment` or matches the debt name, it automatically calculates the remaining principal balance.
4. **Audit**: All actions (creation, payment application) are audited via `AuditableRepository`. 

## Consequences
- The system correctly models loans, money owed, and credit cards.
- A user's net worth calculation (Assets - Liabilities) can now be accurately projected.
- Currently, the engine performs a full recalculation based on historical matches. Future optimizations should leverage a `debt_payment_link` table similar to `transfer_link` to prevent double-counting and reduce computational overhead.
