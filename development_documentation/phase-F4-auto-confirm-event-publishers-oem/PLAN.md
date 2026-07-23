# Phase F4 — Auto-Confirm, Event Publishers, OEM Guidance

## Summary

- Added `autoConfirmEnabled` and `autoConfirmThreshold` flags to `SettingsProvider`.
- Created `ConfidenceTracker` in `core-obligations` — queries `merchant_category_rule` for per-merchant confirmation counts.
- Updated `ObligationDetectionEngine` to use auto-confirm logic: when auto-confirm is enabled AND the merchant has >= threshold confirmations, obligations are auto-created and `RecurringObligationConfirmed` is published. Otherwise, `RecurringObligationProposed` is published.
- Wired `TransferDetectionEngine` to publish `TransferMatched` (on successful link) and `TransferUnmatchedFlagged` (on heuristic match with unconfirmed pair). Updated DI to inject `DomainEventBus`.
- Wired `IncomeRecurrencePatternDetector` to publish `IncomeRecurrencePatternDetected` via new `detectAndPublish()`.
- Created `OemAutostartHelper` in `app` module with manufacturer-specific autostart intent builders and guide steps for Xiaomi/OPPO/Vivo/Huawei.
- Added "Auto-confirm recurring bills" toggle card to Settings screen.
- Skipped `UniversalAuditSubscriber` — the `AuditLog` model uses strong `EntityType`/`AuditAction` enums that don't map to all 23 event types. Existing per-engine audit calls cover the same ground.

## Files changed

| File | Change |
|------|--------|
| `core-ledger/.../config/SettingsProvider.kt` | Added `isAutoConfirmEnabled`/`setAutoConfirmEnabled` and `getAutoConfirmThreshold`/`setAutoConfirmThreshold` |
| `app/.../config/EncryptedSettingsProvider.kt` | Implemented auto-confirm persistence in EncryptedSharedPreferences |
| `core-obligations/.../engine/ConfidenceTracker.kt` | NEW: per-merchant confirmation from `merchant_category_rule` |
| `core-obligations/.../engine/ObligationDetectionEngine.kt` | Auto-confirm logic: create + `RecurringObligationConfirmed` if trusted, `RecurringObligationProposed` otherwise |
| `core-transfer/.../engine/TransferDetectionEngine.kt` | Publishes `TransferMatched` on link, `TransferUnmatchedFlagged` on unconfirmed heuristic |
| `core-transfer/.../di/TransferModule.kt` | Injects `DomainEventBus` into `TransferDetectionEngine` |
| `core-obligations/.../engine/IncomeRecurrencePatternDetector.kt` | Added `detectAndPublish()` that emits `IncomeRecurrencePatternDetected` |
| `core-obligations/.../di/ObligationsModule.kt` | Updated DI for new `SettingsProvider` and `DomainEventBus` deps |
| `feature-dashboard/.../viewmodel/DashboardViewModel.kt` | Switched to `detectAndPublish()` |
| `app/.../util/OemAutostartHelper.kt` | NEW: OEM-specific autostart intent builders + guide steps |
| `feature-settings/.../ui/SettingsScreen.kt` | Added "Automation" card with auto-confirm toggle |
