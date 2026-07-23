# Phase F7 — Encrypted Export & Ingestion Reconciliation

## Summary

- Created `EncryptedExporter` in `app/export/` — AES-256-GCM encryption with PBKDF2 key derivation from user passphrase. Output format: 4-byte header length + JSON header (magic "SCIB", version, salt, IV) + ciphertext.
- Created `EncryptedImporter` in `app/export/` — validates header, derives key from passphrase, decrypts with AES/GCM, replaces DB file with a pre-import backup copy.
- Created `IngestionReconciliationWorker` — periodic CoroutineWorker (6-hour tick) that checks notification listener status and pending event count.
- Scheduled reconciliation worker in `SciuroApp.onCreate()` via `WorkManager.enqueueUniquePeriodicWork`.
- Added "Data Backup" info card to Settings screen (descriptive text — export/import buttons deferred due to module dependency constraints).
- Export/import logic uses existing `security-crypto` and `work-runtime-ktx` dependencies — no new libs.

## Files changed

| File | Change |
|------|--------|
| `app/.../export/EncryptedExporter.kt` | NEW: AES-GCM encrypt DB to file |
| `app/.../export/EncryptedImporter.kt` | NEW: AES-GCM decrypt and restore DB |
| `app/.../worker/IngestionReconciliationWorker.kt` | NEW: periodic notification-listener health check |
| `app/.../SciuroApp.kt` | Schedules reconciliation worker in onCreate |
| `feature-settings/.../ui/SettingsScreen.kt` | Added "Data Backup" info card |
