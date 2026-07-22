# Problems: Phase Acct-1–3

## SQLDelight schema duality
The `.sq` file's `CREATE TABLE` and the `.sqm` migration must both declare new columns. The compiler reads from `.sq`; the runtime migration applies `.sqm`. Initially only the migration was written, causing SQLDelight code generation to fail with "No column found." Fixed by mirroring the column additions in the `CREATE TABLE` statement inside `Account.sq`.

## Cross-module smart casts
Kotlin cannot smart-cast nullable properties from a different module. `ownAccount.account_number` (exposed by SQLDelight-generated code in `:core-ledger`) required explicit `?.let {}` or temporary local val patterns in `:core-transfer`. Each cross-module nullable access needed a workaround — caught at compile time.
