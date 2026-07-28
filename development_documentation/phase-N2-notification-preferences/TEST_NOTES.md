# Test Notes: Phase N2 — User-Configurable Notification Preferences

## Scope

Design and implement a fully user-configurable notification system across Sciuro. Previously, push notifications were either hardcoded or misused existing channels (e.g. `showBudgetAlert()` called for BNPL risk, cash anomalies, and transfer reviews). Every threshold was hardcoded. This phase adds:

- **P1 — Notification Preferences Store**: New `EncryptedSharedPreferences`-backed store (`NotificationPreferencesStore`) that persists per-channel enable/disable state and typed parameters (backup interval, large transaction threshold, debt due days-before).
- **P2 — New Notification Channels**: 8 new Android notification channels: Backup Reminders, Runway Alerts, Spending Alerts, Weekly Digest, BNPL Risk, Cash Anomalies, Transfer Review, Milestones.
- **P3 — New Domain Event**: `NetPositionMilestoneReached` event fired when net worth crosses configurable thresholds (RM 1k/5k/10k/50k/100k/500k), tracked persistently.
- **P4 — P4 Fixes**: `BnplRiskThresholdCrossed`, `CashRecounted`, and `TransferUnmatchedFlagged` now use proper dedicated notification channels instead of hijacking `showBudgetAlert()`.
- **P5 — Consolidated Nightly Check Worker**: Single `NightlyCheckWorker` (WorkManager, periodic daily) checks: backup overdue, runway critical, debts due soon, income not arrived, Sunday weekly digest.
- **P6 — Settings UI**: New "Notifications" section in Settings with 5 group cards (Data Safety, Spending, Reminders, Insights, Risk). Each notification type has an enable/disable toggle. Parametric types (backup interval, large transaction threshold, debt due days-before) have expandable sliders.

## Files new or changed

| File | Change |
|------|--------|
| `feature-settings/.../config/NotificationPreferencesStore.kt` | NEW: EncryptedSharedPreferences store with `isEnabled()`, `getInt()`, `getDouble()` keyed by channel type |
| `core-audit/.../events/DomainEvent.kt` | +`NetPositionMilestoneReached(netWorth, milestone)` |
| `core-ledger/.../config/SettingsProvider.kt` | +`getLastBackupTimestamp`, `setLastBackupTimestamp`, `getLastMilestoneReached`, `setLastMilestoneReached` |
| `core-ledger/.../config/EncryptedSettingsProvider.kt` | Implemented the 4 new methods |
| `core-ledger/.../subscriber/NetPositionSubscriber.kt` | Milestone detection: hardcoded thresholds (1k/5k/10k/50k/100k/500k), persists via SettingsProvider, publishes `NetPositionMilestoneReached` |
| `core-ledger/.../di/LedgerModule.kt` | Updated `NetPositionSubscriber` Koin wiring (3rd param `SettingsProvider`) |
| `core-ledger/.../repository/CategoryRepository.kt` | +`observeCategories()` for weekly digest |
| `app/.../worker/NotificationHelper.kt` | +8 notification channels, 12 new `showXxx()` functions with preference-aware guards |
| `app/.../engine/UniversalEventSubscriber.kt` | P4 fixes (BNPL/Cash/Transfer use proper channels), +`TransactionCategorized` (large txn threshold), +`ObligationCycleSettled` (autopay bill), +`NetPositionMilestoneReached`. All gated by `prefsStore.isEnabled()` |
| `app/.../engine/NotificationSuppressionEngine.kt` | +`NetPositionMilestoneReached` to `isAlwaysNotify()` |
| `app/.../worker/NightlyCheckWorker.kt` | NEW: periodic daily check — backup overdue, runway critical, debts due, income not arrived, Sunday digest |
| `app/.../SciuroApp.kt` | Wired `NotificationPreferencesStore`, updated `UniversalEventSubscriber` params, scheduled `NightlyCheckWorker` |
| `app/.../navigation/SciuroNavGraph.kt` | Backup timestamp saved on successful export via `SettingsProvider.setLastBackupTimestamp()` |
| `feature-settings/.../strings.xml` | +22 new notification preference strings (5 group headers, 13 channel labels/descriptions) |
| `feature-settings/.../viewmodel/SettingsViewModel.kt` | +15 notification preference state fields + toggle/setter methods |
| `feature-settings/.../ui/SettingsScreen.kt` | New "Notifications" section: 5 group cards with expandable config sliders, reuses existing `AnimatedVisibility` pattern |
| `feature-settings/.../di/SettingsModule.kt` | Updated `SettingsViewModel` Koin wiring (2nd param `NotificationPreferencesStore`) |
| `feature-settings/build.gradle.kts` | +`libs.androidx.security.crypto` dependency |

## Verification

1. **Build**: `./gradlew :app:compileDebugKotlin` — passes with no errors.
2. **Notification preferences load**: Open Settings > scroll to "Notifications". All 13 channel toggles should default to ON.
3. **Toggle persistence**: Toggle any channel OFF, background app, reopen Settings — toggle should remain OFF.
4. **Backup interval slider**: Settings > Notifications > Data Safety > Backup Reminder — expand, drag slider from 1 to 30, value label updates.
5. **Large transaction threshold slider**: Settings > Spending > Large Transaction — expand, drag slider, threshold displays as "RM X".
6. **Debt due days-before slider**: Settings > Reminders > Debt Due Soon — expand, drag slider from 1 to 30.
7. **Net position milestone**: Create accounts + investments exceeding RM 1,000 → milestone notification should fire once (persisted across restarts).
8. **Backup timestamp**: Export backup from Settings > Data Backup → subsequent NightlyCheckWorker should not fire "backup overdue" for 7 days.
9. **Legacy channels unchanged**: Existing `sciuro_review_channel`, `sciuro_budget_channel`, `sciuro_bill_channel`, `sciuro_debt_channel`, `sciuro_obligation_channel` still functional.
