# Phase F1 — Configurable Allowlist & Parser Health Metrics

## Summary

- Refactored hardcoded `IngestionConfig` into `IngestionDefaults` + `MutableIngestionAllowlist` backed by `SettingsProvider`.
- `SciuroNotificationService` now reads from the runtime allowlist instead of a hardcoded `object`.
- `DeveloperTabSources` now has an enabled "Add Custom Package" field with remove buttons on each row.
- Added SQLDelight queries `selectMatchRateByPackage` and `selectMatchRateByPackageRecent` to `raw_event_staging`.
- Added `ParserHealthRepository` exposing per-package match rates over a configurable time window.
- Added `DeveloperTabHealth` tab to `DeveloperSettingsScreen` showing per-package match rates with degradation warnings.

## Files changed

| File | Change |
|------|--------|
| `core-ledger/.../config/SettingsProvider.kt` | Added 4 allowlist persistence methods |
| `app/.../config/EncryptedSettingsProvider.kt` | Implemented allowlist persistence in EncryptedSharedPreferences |
| `core-ingestion/.../config/IngestionConfig.kt` | Renamed `IngestionConfig` to `IngestionDefaults`; extracted semicolon comments to keep the file clean |
| `core-ingestion/.../config/MutableIngestionAllowlist.kt` | NEW: runtime-editable allowlist wrapping SettingsProvider |
| `core-ingestion/.../di/IngestionModule.kt` | Registered `MutableIngestionAllowlist` as Koin single |
| `core-ingestion/.../service/SciuroNotificationService.kt` | Switched from `IngestionConfig.allowedPackages` to `allowlist.allows()` |
| `core-ledger/.../db/RawEventStaging.sq` | Added `selectMatchRateByPackage` and `selectMatchRateByPackageRecent` queries |
| `core-parsing/.../metrics/ParserHealthRepository.kt` | NEW: repository for per-package match-rate aggregation |
| `core-parsing/.../di/ParsingModule.kt` | Registered `ParserHealthRepository` as Koin single |
| `feature-settings/.../ui/DeveloperTabSources.kt` | Enabled "Add Custom Package" field, added remove buttons, switched to runtime allowlist |
| `feature-settings/.../ui/DeveloperTabHealth.kt` | NEW: per-package match rate display with degradation warnings |
| `feature-settings/.../ui/DeveloperSettingsScreen.kt` | Added "Health" tab |
| `core-ingestion/build.gradle.kts` | Added `commonTest` source set |
