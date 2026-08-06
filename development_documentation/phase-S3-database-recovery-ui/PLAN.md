# Phase S3 — Database Recovery Interstitial & Weekly Integrity Checks

## Summary

Phase S1 quarantined the database instead of deleting it, but the user still launched into a
silent empty database with no explanation. This phase makes quarantine visible and recoverable:

1. **Full-screen recovery interstitial** — when a quarantined (`sciuro.db.quarantined.*`) database
   exists, launch shows "Your previous data could not be opened. It has been preserved" with two
   actions: **Import a backup** or **Start fresh**. The app does NOT proceed silently to an empty
   dashboard.
2. **Quarantine counters** — `quarantine_count`, `last_quarantine_timestamp`, and a recovery
   acknowledgment flag persisted in `EncryptedSharedPreferences` (`sciuro_db_secure_prefs`), plus a
   "Database Recovery" card in the Developer Health tab.
3. **Weekly integrity verification** — `NightlyCheckWorker` runs `PRAGMA integrity_check` at most
   once per week and logs the outcome to `pipeline_trace` (`DATABASE_INTEGRITY` stage).

## Design decisions

- **Eager validation before Koin.** The two-phase quarantine check moved out of the lazy
  `single<SqlDriver>` factory into `DatabaseRecoveryManager.validateDatabaseOnStartup()`, invoked at
  the very top of `SciuroApp.onCreate()`. Otherwise the recovery gate (which must read quarantine
  state before the driver exists) could not see a quarantine that only happens during lazy driver
  creation — the first corrupt launch would still silently produce an empty DB.
- **Startup gating.** While a quarantine awaits a decision, `SciuroApp` does not start the ingestion
  orchestrator, rule learner, subscribers, or periodic workers, and cancels any previously-scheduled
  `ingestion_reconciliation` / `nightly_check` / `ReviewReminder` work. Nothing writes into a fresh
  DB behind the interstitial.
- **Quarantine metadata lives in `sciuro_db_secure_prefs`** (the DB-key store), not
  `sciuro_secure_settings`, because it describes DB lifecycle. Both are encrypted.
- **"Start fresh" and "Import backup" both acknowledge** the pending state so the interstitial does
  not reappear, then restart `MainActivity` via `CLEAR_TASK`. Import reuses the existing
  `EncryptedImporter` (v1/v2 backups, key recovery). Quarantined files are never deleted by either
  path.
- **Contact support skipped** — no support email/URL exists in the repo; the user chose to omit it
  for this phase rather than fabricate one.
- **Weekly schedule, not daily.** The daily `NightlyCheckWorker` is reused; integrity runs only when
  `IntegrityCheckPolicy.isDue(lastCheck, now)` (7-day interval). Failures are logged + traced, never
  auto-quarantined.

## Files changed

| File | Change |
|------|--------|
| `core-ledger/.../security/QuarantineFiles.kt` (new, commonMain) | Quarantine filename pattern + timestamp parse helpers (pure, testable). |
| `core-ledger/.../security/IntegrityCheckPolicy.kt` (new, commonMain) | Weekly-due predicate. |
| `core-ledger/.../security/DatabaseRecoveryManager.kt` (new, androidMain) | Startup validation, quarantine operation (rename + sidecar cleanup + encrypted metadata), recovery-pending state, integrity metadata. |
| `core-ledger/.../security/DatabaseIntegrityChecker.kt` (new, androidMain) | Read-only `PRAGMA integrity_check` against the SQLCipher DB. |
| `core-ledger/.../di/PlatformDatabaseModule.kt` | Two-phase validation delegated to `DatabaseRecoveryManager`; registers it in Koin. |
| `core-audit/.../trace/TraceStage.kt` | Added `DATABASE_INTEGRITY`. |
| `feature-settings/.../ui/DatabaseRecoveryScreen.kt` (new) | Full-screen interstitial with preserved-data summary, import + start-fresh actions. |
| `feature-settings/.../ui/BackupPasswordDialog.kt` (new) | Extracted shared password dialog (reused by DataSettings + recovery screens). |
| `feature-settings/.../ui/DataSettingsScreen.kt` | Uses the shared `BackupPasswordDialog`. |
| `feature-settings/.../viewmodel/DeveloperSettingsViewModel.kt` | Injected `DatabaseRecoveryManager`; exposes `DatabaseRecoveryMetrics`. |
| `feature-settings/.../di/SettingsModule.kt` | ViewModel registration updated. |
| `feature-settings/.../ui/DeveloperTabHealth.kt` | "Database Recovery" card (quarantine count, last quarantine, preserved files, last integrity check/result). |
| `feature-settings/.../res/values/strings.xml` | Recovery screen + health strings. |
| `app/.../recovery/DatabaseRecoveryGate.kt` (new) | Renders the interstitial when recovery is pending; wires import/start-fresh; restarts the app. |
| `app/.../MainActivity.kt` | Recovery gate wraps content; permission prompt + `ReviewReminder` scheduling gated. |
| `app/.../SciuroApp.kt` | Eager `validateDatabaseOnStartup()`; startup + workers gated on pending recovery. |
| `app/.../worker/NightlyCheckWorker.kt` | Weekly integrity check traced to `pipeline_trace`. |
| `core-ledger/build.gradle.kts` | Added `jvmTest` dependencies (also fixes the existing `AuditAtomicityTest` which needed the JDBC drivers). |
| `core-ledger/src/jvmTest/.../security/QuarantineFilesTest.kt` (new) | Filename pattern + timestamp round-trip tests. |
| `core-ledger/src/jvmTest/.../security/IntegrityCheckPolicyTest.kt` (new) | Weekly-due boundary tests. |

## Verification

- `./gradlew :core-ledger:jvmTest --console=plain`
- `./gradlew :app:compileDebugKotlin --console=plain`
- Manual UAT in `TEST_NOTES.md`: key-loss quarantine, corruption quarantine, import recovery,
  start-fresh preservation, Developer Health card, weekly trace.
