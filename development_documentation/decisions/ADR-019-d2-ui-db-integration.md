# ADR 019: Connecting UI to Database Flows

## Context
With the structural UI in place across `feature-*` modules, we needed to swap the mock ViewModel `StateFlow` data with actual live observations from the `core-*` databases built in earlier phases.

## Decision
1. **Flow Exposure**: Added `.asFlow().mapToList(Dispatchers.Default)` extensions to `AccountRepository`, `TransactionRepository`, and `BudgetRepository` to make SQDelight queries fully reactive.
2. **ViewModel Mapping**: Updated all feature ViewModels to consume these flows and map the database domain models into UI models (e.g., mapping unreviewed transactions to "TODO" Kanban tasks).
3. **Cross-Module Boundaries**: Ensured `core-audit` dependency was exposed via `api` in `core-ledger` and `core-budget` to prevent build issues when `feature-*` modules attempted to resolve the `AuditableRepository` superclass.

## Consequences
- The application UI is now genuinely reactive. Any background ingestion of a CSV that creates unreviewed transactions will instantly populate the Kanban "TODO" column, and any account balance updates will reflect on the Dashboard immediately.
- Mock data has been completely eliminated from the production build path.
