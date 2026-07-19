# Test Notes: Phase A5 - Financial Taxonomy & Data Model

## Scope
- Defined SQLDelight schemas (`Account`, `Category`, `TransactionRecord`).
- Created Koin-injected Repositories (`AccountRepository`, `CategoryRepository`, `TransactionRepository`).
- Extended `AuditableRepository` for ledger mutations.

## Results
- `core-ledger` builds successfully.
- SQLDelight successfully generated the `SciuroDatabase` interfaces across all targets.

## Excluded
- Database migrations are out of scope until v2 schema changes are needed.
- Complex multi-currency conversion is excluded (defaults to MYR).
