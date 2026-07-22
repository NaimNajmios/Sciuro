# Problems: Phase Transfer-1–2

## Circular dependency prevention
`TransferDetectionEngine` could not be injected into `TransactionRepository` because `TransferRepository` (used by the engine) depends on `TransactionRepository`. Solved by wiring the engine at the orchestrator level (`SciuroIngestionOrchestrator`) instead, where the draft is available and the dependency direction is one-way.

## `sorted()` on nullable strings
`listOf(outflowTx.account_id, inflowTx.account_id).sorted()` fails because both values are `String?` and `String?` doesn't implement `Comparable`. Fixed with `listOfNotNull(...)`.

## Cross-module smart casts (recurring)
Same pattern as Acct phase — `match.account_id` (a cross-module nullable) required a local val before the null check + `isPairConfirmed` call. Kotlin's module-level smart cast restriction meant these weren't caught until compile time.

## False positive: "Card ending XXXX" for Maybank card SMS
The `endingAccountNumberRegex` pattern `(?:ending|berakhir)\s+([\d*Xx]{4,20})` matches "ending 1234" in the Maybank card SMS fixture ("Your Card ending 1234 was used at MCDONALDS"). This is a mild false positive — the number is a card ending, not an account ending. Accepted as low risk since:
- The downstream `matchesAccountSuffix()` compares against registered `account_number` values only. Card numbers won't match unless the user also registered a card number as an account (unlikely).
- Adding a negative lookbehind for "Card" would make the regex fragile and bank-specific.
- The confidence boost (+0.1f) is small enough that it doesn't push low-confidence parses over the threshold on its own.

## JVM target cascade
Adding `jvm()` to `core-transfer` for in-memory SQLite testing required `jvm()` targets on `core-ledger` and `core-audit` as well (transitive dependencies). This is standard KMP practice but was a one-time setup cost across three modules, including adding JVM `actual` implementations for `currentTimeMillis()` and `generateUuid()` in `core-audit`.
