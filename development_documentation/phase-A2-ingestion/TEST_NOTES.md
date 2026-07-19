# Test Notes: Phase A2 - Ingestion Framework

## Scope
- Defined `RawEvent` and `IngestionSource`.
- Implemented `NotificationSourceAdapter` with a `SharedFlow` buffer.
- Configured allowlist in `IngestionConfig`.
- Created Android `SciuroNotificationService` to capture broadcasts.

## Results
- `core-ingestion` module builds successfully.
- Proper separation between the KMP-compatible adapter (`commonMain`) and the Android-specific Service (`androidMain`).

## Excluded
- Real device notification testing is deferred until the app can actually process and display the events.
