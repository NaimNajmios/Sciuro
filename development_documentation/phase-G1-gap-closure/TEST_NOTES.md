# Phase G1 — Polish & Reliability Gap Closure

## Summary

Implemented 4 of the 6 remaining gap items from the comprehensive gap analysis:
- **SMS ingestion adapter** with `SmsReceiver` BroadcastReceiver
- **NewFinanceAppDetected** package install observer with notification suggestion
- **Battery optimisation exemption** via onboarding step + Settings card + OEM autostart guide
- **Cash adjustment remarks** with anomaly-aware prompts in the bottom sheet
- **Quiet hours** toggle with `NotificationSuppressionEngine` + budget alert notifications

Deferred: Lottie mascot assets, pull-to-refresh, shared-element transitions (visual polish — require design assets).

---

## Test Notes

### G1.1 — SMS & Email Ingestion Adapters

- [PASS] `MultiplexIngestionSource` compiles and correctly merges multiple `IngestionSource` flows via `kotlinx.coroutines.flow.merge`
- [PASS] `SmsSourceAdapter` follows the same `MutableSharedFlow` pattern as `NotificationSourceAdapter` (buffer capacity 50)
- [PASS] `SmsReceiver` registers for `SMS_RECEIVED`, filters by allowlist, detects financial signals (RM, transaction, transfer, credited, debited, payment, receipt), persists to staging table, and emits to the adapter
- [PASS] `SciuroIngestionOrchestrator` refactored from concrete `NotificationSourceAdapter` to `MultiplexIngestionSource` — no downstream breakage
- [PASS] `ClassifierModule` and `IngestionModule` updated with new DI wiring
- [PASS] `RECEIVE_SMS` permission added to AndroidManifest
- [PASS] `assembleDebug` succeeds
- [PASS] `testDebugUnitTest` succeeds
- [DEFERRED] Direct email API adapter — `AggregatorForwardMatcher` already handles forwarded emails via notification interception

### G1.2 — NewFinanceAppDetected (Package Install Observer)

- [PASS] `FinanceAppInstallReceiver` triggers on `PACKAGE_ADDED`/`PACKAGE_REPLACED` and enqueues one-time `FinanceAppCheckWorker`
- [PASS] `FinanceAppCheckWorker` validates package against `IngestionDefaults.knownFinanceAppSignatures` (52 MY finance apps) and publishes `DomainEvent.NewFinanceAppDetected`
- [PASS] `FinanceAppSuggestionSubscriber` collects from `DomainEventBus`, creates notification with deep-link to Settings > Sources tab
- [PASS] Receiver registered in AndroidManifest with `PACKAGE_ADDED`/`PACKAGE_REPLACED` intent filter
- [PASS] Subscriber bootstrapped in `SciuroApp.onCreate()`
- [PASS] `assembleDebug` succeeds
- [NOTE] On Android 13+, manifest-declared package-install receivers may not fire due to background launch restrictions. Runtime registration via `registerReceiver()` is a future hardening item.

### G1.3 — Battery Optimisation Exemption

- [PASS] `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` added to AndroidManifest
- [PASS] `OnboardingBatteryScreen` created with rationale text, OEM-specific guide card, "Allow Background Activity" button (launches `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), autostart button (for aggressive OEMs), and "Skip for now" option
- [PASS] Battery step inserted in onboarding flow between notification permission gate and wallet setup — shows only if battery optimization is active
- [PASS] "Background Reliability" card added to Settings with live battery optimization status, fix button, and autostart settings button
- [PASS] `OemAutostartHelper` moved from `app` module to `core-ui` module for cross-module access
- [PASS] Oppo/Realme bug fixed: `FloatWindowListActivity` → `StartupAppListActivity`
- [PASS] `SettingsScreen` updated with `OemAutostartHelper` import from `core-ui.util`
- [PASS] `assembleDebug` succeeds
- [MANUAL] Battery optimization settings screen tested manually on device
- [NOTE] Google Play may reject `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for non-messaging/VoIP apps. Rationale screen improves odds but submission is at reviewer discretion.

### G1.6a — Anomaly-Aware Remark Prompts

- [PASS] Migration `9.sqm` adds `remark TEXT` column to `cash_adjustment` table
- [PASS] `CashAdjustment.sq` updated: `remark` in CREATE TABLE and `insertAdjustment` query
- [PASS] `CashAdjustmentRepository.createAdjustment()` accepts `remark: String? = null`
- [PASS] `ReconciliationEngine` updated to pass `remark = null` in direct `insertAdjustment` call
- [PASS] `AdjustmentBottomSheet` updated: `onConfirm(amount, reason, remark)`, large variance prompt (>= RM 50) with `SignalWarning`-colored message and optional remark text field
- [PASS] `AccountDetailViewModel.recordCorrection()` and `WalletViewModel.recordCorrection()` updated with `remark` parameter (default null for backward compat)
- [PASS] `AccountDetailScreen` passes remark through to ViewModel
- [PASS] `assembleDebug` succeeds
- [PASS] `testDebugUnitTest` succeeds
- [PASS] `MutableIngestionAllowlistTest.FakeSettingsProvider` updated with all missing interface stubs (quiet hours, auto-confirm, manual prices)

### G1.6b — Runway-Aware Quiet Hours + Budget Notifications

- [PASS] `SettingsProvider` extended with `isQuietHoursEnabled()`, `getQuietHoursStart()`, `getQuietHoursEnd()` and setters
- [PASS] `EncryptedSettingsProvider` implements all 6 new methods (default: off, 22:00–07:00)
- [PASS] Quiet hours card added to SettingsScreen: toggle switch + start/end hour adjustment buttons
- [PASS] `NotificationSuppressionEngine` created — subscribes to `DomainEventBus`, handles `BudgetThresholdCrossed`, checks quiet hours and runway health
- [PASS] Suppression logic: if quiet hours enabled and current hour within range → suppress; if runway < 0 (critical) → always escalate regardless of quiet hours
- [PASS] `NotificationHelper` refactored: 3 channels (`sciuro_review_channel`, `sciuro_budget_channel`, `sciuro_bill_channel`), added `showBudgetAlert()` and `showBillReminder()`
- [PASS] `NotificationSuppressionEngine` registered in `appModule` Koin and bootstrapped in `SciuroApp.onCreate()`
- [PASS] `assembleDebug` succeeds

### Build Verification

- [PASS] `./gradlew detekt` — passes
- [PASS] `./gradlew assembleDebug` — succeeds
- [PASS] `./gradlew testDebugUnitTest` — 318 tasks, all succeed
- [SKIP] `./gradlew allTests` — pre-existing JVM 21 target issue (unrelated to this phase)
- [SKIP] `./gradlew :core-transfer:jvmTest` — same pre-existing JVM 21 issue

---

## Files Changed

### New files (12)
| File | Purpose |
|---|---|
| `core-ingestion/.../source/MultiplexIngestionSource.kt` | Composite adapter merging multiple IngestionSource flows |
| `core-ingestion/.../source/sms/SmsSourceAdapter.kt` | SMS source adapter (commonMain) |
| `core-ingestion/.../receiver/SmsReceiver.kt` | BroadcastReceiver for SMS_RECEIVED (androidMain) |
| `app/.../receiver/FinanceAppInstallReceiver.kt` | BroadcastReceiver for PACKAGE_ADDED/PACKAGE_REPLACED |
| `app/.../worker/FinanceAppCheckWorker.kt` | CoroutineWorker validating installed finance apps |
| `app/.../subscriber/FinanceAppSuggestionSubscriber.kt` | Subscriber that shows notification on new finance app |
| `feature-wallet/.../ui/OnboardingBatteryScreen.kt` | Battery optimisation rationale + action screen |
| `app/.../engine/NotificationSuppressionEngine.kt` | Quiets hours + runway-aware notification gate |
| `core-ui/.../util/OemAutostartHelper.kt` | OEM autostart helper (moved from app module) |
| `core-ledger/.../db/9.sqm` | Migration: add remark column to cash_adjustment |

### Modified files (15)
| File | Change |
|---|---|
| `SciuroIngestionOrchestrator.kt` | `NotificationSourceAdapter` → `MultiplexIngestionSource` |
| `IngestionModule.kt` | Register `SmsSourceAdapter` + `MultiplexIngestionSource` |
| `ClassifierModule.kt` | Wire orchestrator with multiplex source |
| `IngestionConfig.kt` | Add `knownFinanceAppSignatures` map (52 apps) |
| `CashAdjustment.sq` | Add `remark` column + update insert query |
| `CashAdjustmentRepository.kt` | Add `remark` parameter to `createAdjustment()` |
| `ReconciliationEngine.kt` | Pass `remark = null` in direct insert |
| `AdjustmentBottomSheet.kt` | Large variance prompt + remark field |
| `AccountDetailViewModel.kt` | `remark` parameter in `recordCorrection()` |
| `WalletViewModel.kt` | `remark` parameter in `recordCorrection()` |
| `SettingsProvider.kt` | 6 quiet hours methods |
| `EncryptedSettingsProvider.kt` | Quiet hours implementation |
| `NotificationHelper.kt` | 3 channels + budget/bill notification methods |
| `SciuroApp.kt` | Bootstrap `FinanceAppSuggestionSubscriber` + `NotificationSuppressionEngine` |
| `MainActivity.kt` | Battery step in onboarding flow |
| `SettingsScreen.kt` | Background reliability card + quiet hours card |
| `AndroidManifest.xml` | RECEIVE_SMS, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, SmsReceiver, FinanceAppInstallReceiver |
| `MutableIngestionAllowlistTest.kt` | Add missing interface stubs |

### Deleted files (1)
| File | Reason |
|---|---|
| `app/.../util/OemAutostartHelper.kt` | Moved to `core-ui` for cross-module access |
