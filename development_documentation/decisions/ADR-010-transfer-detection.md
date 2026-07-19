# ADR 010: Transfer Detection Engine

## Context
A major pain point in personal finance tracking is transferring money between owned accounts (e.g., Bank -> eWallet). Without intervention, the ledger records an expense and an income, artificially inflating the user's spending metrics. Sciuro needs a way to detect and link these dual transactions into a single "Transfer" event.

## Decision
1. **Schema**: Added `transfer_link` to `SciuroDatabase` in `core-ledger`. This table maps `outflow_transaction_id` and `inflow_transaction_id` together.
2. **TransferDetectionEngine**: Built in `core-transfer`. It scans unlinked transactions looking for a pairing:
   - One INFLOW and one OUTFLOW.
   - Exact same amount.
   - Occurring within a 2-minute time window.
3. **Re-Categorization**: When a pair is found, it inserts the `TransferLink` and automatically calls the `TransactionRepository` to re-categorize both the INFLOW and OUTFLOW as `cat_transfer`.
4. **Audit**: This operation is fully audited, marking both the link creation and the re-categorization under `AuditSource.SYSTEM_AUTO`.

## Consequences
- Accurately tracks money moving between internal accounts without corrupting budget metrics.
- The 2-minute time window relies on notifications arriving promptly. If network latency delays a push notification, the detection might miss the transfer. Future iterations may expand this window or use fuzzy matching.
