# ADR 017: Dashboard & Wallet UI 

## Context
As the primary entry point for Sciuro, the Home Dashboard and Wallet screens must accurately represent the state of the underlying `core-ledger`. The UI needs to act as a reactive consumer of the intelligence engines built in Phase B.

## Decision
1. **Module Creation**: Scaffolded `feature-dashboard` and `feature-wallet` with Jetpack Compose Multiplatform dependencies (targeted to Android).
2. **Architecture**: 
   - Created `DashboardViewModel` (maintaining reactive `DashboardState` with Net Worth, Inbox Count, and Budgets).
   - Created `WalletViewModel` (maintaining reactive state of `WalletAccount` containing Bank and e-Wallet splits).
3. **UI Implementation**: Built Jetpack Compose screens for both. `DashboardScreen` offers high-level aggregates while `WalletScreen` displays detailed liquidity breakdowns.
4. **Mocking (Phase C2)**: For scaffolding speed, the view models currently return hardcoded mock data. 

## Consequences
- The application now has a structural UI layer to present financial standing.
- In the integration phase, we simply need to swap the ViewModel `MutableStateFlow` assignments with reactive `Flow` outputs from `core-ledger` repositories.

## Update (Phase C Finalization)
- Replaced mock data with real flows driven by `TransactionRepository` and `AccountRepository`.
- Refactored `DashboardScreen` to use `SwipeToDismissBox` for transaction inbox triage, paired with account selection prompts to enforce strict ledger matching.
- Upgraded `WalletScreen` with Compose `HorizontalPager`, eliminating static list constraints and permitting dynamic rendering of transactions tailored to actively swiped accounts.
- Resolved IME keyboard interference using `adjustResize` and strict `imePadding()` modifiers on BottomSheets.
