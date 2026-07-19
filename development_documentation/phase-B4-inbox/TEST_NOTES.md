# Test Notes: Phase B4 - Manual Review Inbox

## Scope
- Exposed reactive Kotlin `Flow` for unreviewed transactions from `TransactionRepository`.
- Prepared the data and domain layer for UI integration.

## Results
- `core-ledger` compiles successfully with `sqldelight-coroutines-extensions`.
- The Flow correctly maps SQLite driver updates to Kotlin Flow emissions using `Dispatchers.Default`.

## Excluded
- The actual Jetpack Compose UI (Screens, ViewModels) is explicitly excluded and deferred to Phase C (Application Assembly).
