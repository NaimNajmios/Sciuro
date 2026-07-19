# ADR 007: Financial Taxonomy and Ledger Schema

## Context
With the ingestion pipeline outputting `StructuredDraft` objects, Sciuro needs a finalized ledger destination. The ledger must distinguish between the raw parsed data and the officially booked transaction, while tracking the account balances automatically.

## Decision
1. **Separation of Drafts vs. Records**: `StructuredDraft` lives purely in memory as part of the ingestion/parsing pipeline. When booked, it becomes a `Transaction` in the SQLite database (`transaction_record`).
2. **Review Inbox Pattern**: Transactions parsed by an LLM or missing a merchant/category are inserted with `is_reviewed = 0`. This flags them for the "Manual Review Inbox" UI.
3. **Atomic Balance Updates**: `TransactionRepository` modifies the `account` balance and inserts the `transaction_record` within the same `withAudit` block, guaranteeing that the ledger delta perfectly matches the transaction amount.
4. **Audit Inheritance**: All repository actions (create account, book transaction) extend `AuditableRepository`. This guarantees every ledger mutation is historically reproducible.

## Consequences
- We have a clear barrier between "messy external notification data" and "clean internal financial records".
- KMP Koin injection wires the SQLDelight DAOs into the Repositories, isolating the platform-specific SQLite drivers from the shared business logic.
