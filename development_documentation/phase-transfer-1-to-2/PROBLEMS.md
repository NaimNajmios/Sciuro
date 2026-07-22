# Problems: Phase Transfer-1–2

## Circular dependency prevention
`TransferDetectionEngine` could not be injected into `TransactionRepository` because `TransferRepository` (used by the engine) depends on `TransactionRepository`. Solved by wiring the engine at the orchestrator level (`SciuroIngestionOrchestrator`) instead, where the draft is available and the dependency direction is one-way.

## `sorted()` on nullable strings
`listOf(outflowTx.account_id, inflowTx.account_id).sorted()` fails because both values are `String?` and `String?` doesn't implement `Comparable`. Fixed with `listOfNotNull(...)`.

## Cross-module smart casts (recurring)
Same pattern as Acct phase — `match.account_id` (a cross-module nullable) required a local val before the null check + `isPairConfirmed` call. Kotlin's module-level smart cast restriction meant these weren't caught until compile time.
