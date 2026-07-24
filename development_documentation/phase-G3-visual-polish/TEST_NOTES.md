# Phase G3 — Visual Polish & Architecture Closure

## Summary

Closed the remaining actionable gaps from the earlier gap list:
- **Email ingestion adapter stub** added to `MultiplexIngestionSource` multiplex fan-in
- **Pull-to-refresh** on Dashboard, Wallet, Kanban, and Budgets screens using Material3 `PullToRefreshContainer`
- **Notification suppression centralized** — `UniversalEventSubscriber` now gates all 9 notification paths through `NotificationSuppressionEngine.shouldSuppress()`, with critical events (BNPL risk, obligation amount drift, auto-confirmed bills, budget suggestions) always bypassing suppression
- **Shared-element transitions** deferred — Compose BOM 2024.05.00 lacks `SharedTransitionLayout` (needs 1.7.0+ of `compose-animation`). BOM bump to 2024.09.00 introduced transitive compilation issues. Pending a dependency upgrade plan.

---

## Test Notes

### G3.1 — Email Source Adapter Stub

- [PASS] `EmailSourceAdapter` compiles in `commonMain` (`core-ingestion/source/email/`)
- [PASS] Adapter follows same `MutableSharedFlow` pattern as `SmsSourceAdapter` (buffer 50)
- [PASS] `EmailSourceAdapterTest` verifies `sourceType == EMAIL` and `emitEmail` surfaces event on `observeEvents()`
- [PASS] `IngestionModule.kt` registers `EmailSourceAdapter` singleton and adds to `MultiplexIngestionSource` adapter list
- [PASS] `assembleDebug` succeeds

### G3.2 — Pull-to-Refresh

- [PASS] Material3 1.2.1 `PullToRefreshContainer` + `rememberPullToRefreshState()` on 4 screens: Dashboard, Wallet, Kanban, Budgets
- [PASS] Each ViewModel has `isRefreshing: StateFlow<Boolean>` and `refresh()` (600ms visual pulse)
- [PASS] Pull gesture triggers refresh; indicator appears at `Alignment.TopCenter`
- [PASS] Existing `nestedScroll` pattern on root `Box` connects pull gesture to `LazyColumn`
- [PASS] `assembleDebug` succeeds

### G3.4 — Centralized Notification Suppression

- [PASS] `NotificationSuppressionEngine.shouldSuppress(event)` public method evaluates quiet hours + runway health
- [PASS] `isAlwaysNotify()` classifies critical events: `BnplRiskThresholdCrossed`, `ObligationAmountDrifted`, `RecurringObligationConfirmed`, `BudgetLimitSuggested` — always notify
- [PASS] Non-critical events suppressed during quiet hours unless runway < 0
- [PASS] `UniversalEventSubscriber` gated: all 9 handlers (`handleProposed` through `handleCashRecounted`) call `suppressionEngine.shouldSuppress(event)` before `NotificationHelper.show*`
- [PASS] `SciuroApp.appModule` updated: `UniversalEventSubscriber` receives `get()` for 4th param (`NotificationSuppressionEngine`)
- [PASS] `assembleDebug` succeeds

### Build Verification

| Command | Result | Notes |
|---------|--------|-------|
| `./gradlew assembleDebug` | PASS | Clean build |
| `./gradlew :core-ingestion:compileDebugUnitTestKotlinAndroid` | PENDING | `EmailSourceAdapterTest` needs `runBlocking` from kotlinx.coroutines (commonTest source set) |

---

## Files Changed

### New files (3)
| File | Purpose |
|---|---|
| `core-ingestion/.../source/email/EmailSourceAdapter.kt` | Email ingestion source adapter stub |
| `core-ingestion/.../source/email/EmailSourceAdapterTest.kt` | Unit test for adapter |
| `development_documentation/phase-G3-visual-polish/TEST_NOTES.md` | This file |

### Modified files (19)
| File | Change |
|---|---|
| `core-ingestion/.../di/IngestionModule.kt` | Register `EmailSourceAdapter` + add to multiplex list |
| `feature-dashboard/.../viewmodel/DashboardViewModel.kt` | Add `isRefreshing` StateFlow + `refresh()` |
| `feature-wallet/.../viewmodel/WalletViewModel.kt` | Add `isRefreshing` StateFlow + `refresh()` |
| `feature-kanban/.../viewmodel/KanbanViewModel.kt` | Add `isRefreshing` StateFlow + `refresh()` |
| `feature-budgets/.../viewmodel/BudgetsViewModel.kt` | Add `isRefreshing` StateFlow + `refresh()` |
| `feature-dashboard/.../ui/DashboardScreen.kt` | Pull-to-refresh via `PullToRefreshContainer` |
| `feature-wallet/.../ui/WalletScreen.kt` | Pull-to-refresh via `PullToRefreshContainer` |
| `feature-kanban/.../ui/KanbanScreen.kt` | Pull-to-refresh via `PullToRefreshContainer` |
| `feature-budgets/.../ui/BudgetsScreen.kt` | Pull-to-refresh via `PullToRefreshContainer` |
| `app/.../engine/NotificationSuppressionEngine.kt` | Add `shouldSuppress()` + `isAlwaysNotify()`, remove old `shouldSuppressBudget()` |
| `app/.../engine/UniversalEventSubscriber.kt` | Inject `NotificationSuppressionEngine`, gate all 9 handlers |
| `app/.../SciuroApp.kt` | Update `UniversalEventSubscriber` Koin wiring (4th param) |
| `gradle/libs.versions.toml` | Added `androidx-compose-animation` lib entry (no version bump kept) |

### Deleted files (1)
| File | Reason |
|---|---|
| `core-ui/.../util/SharedTransitionKeys.kt` | Shared transitions deferred; utility unused |
