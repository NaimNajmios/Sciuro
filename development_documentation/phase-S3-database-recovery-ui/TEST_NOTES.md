# Phase S3 — Database Recovery Interstitial & Weekly Integrity Checks: TEST_NOTES

## Automated tests

Command: `./gradlew :core-ledger:jvmTest --console=plain`

| Test | Result |
|------|--------|
| `QuarantineFilesTest` — filename pattern (`sciuro.db.quarantined.<ts>`), embedded timestamp, malformed-suffix tolerance, non-quarantine names (wal/shm/other DBs) | PASS (5 tests) |
| `IntegrityCheckPolicyTest` — never-checked-is-due, not-due before 7d, due exactly at 7d, due after 7d, freshly-checked-not-due | PASS (5 tests) |
| Pre-existing `AuditAtomicityTest` (needed the newly added `jvmTest` JDBC driver deps) | PASS (4 tests) |

Total: 13 tests passing. Note: `IntegrityCheckPolicy.isDue` treats a zero/negative last-check as
"never checked → due" (added after the first test run exposed the boundary case).

## Compile verification

- `./gradlew :core-ledger:jvmTest` — BUILD SUCCESSFUL
- `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (all modules transitively compiled:
  core-audit, core-ledger, feature-settings, app). Only pre-existing warnings remain.

## Manual UAT checklist (device/emulator)

| Scenario | Steps | Expected |
|----------|-------|----------|
| Key-loss quarantine | Delete `sciuro_db_secure_prefs` (or clear app data but keep `sciuro.db` via ADB push), launch | DB renamed to `sciuro.db.quarantined.<ts>`; full-screen recovery interstitial shown; no onboarding/dashboard. |
| Corruption quarantine | Corrupt `sciuro.db` bytes, launch | Open fails → quarantined; interstitial shown. |
| Import a backup | From interstitial, pick a v2 `.scib` backup + passphrase | Restore succeeds; app restarts into the recovered dashboard; interstitial gone. |
| Start fresh | Tap Start fresh → confirm | App restarts into empty-db onboarding; `sciuro.db.quarantined.*` files remain on disk. |
| Interstitial not sticky | After Start fresh, force-stop + relaunch | No interstitial (quarantined files remain but recovery acknowledged). |
| Developer Health card | Settings → Developer Options → Health | "Database Recovery" shows quarantine count, last quarantine date, preserved-file count, last integrity check/result. |
| Weekly integrity trace | Force `NightlyCheckWorker` with lastIntegrityCheckMs=0 | `pipeline_trace` row with stage `DATABASE_INTEGRITY`, outcome SUCCESS, detail `{result=ok}`; Health shows last check timestamp + result. |
| No background writes while pending | While interstitial visible, check `databases/` | No fresh `sciuro.db` until the user acts (created on import or start-fresh restart). |

## Notes / edge cases

- **Backup password dialog reused**: the private dialog was extracted to a shared
  `BackupPasswordDialog` used by both the Data & Privacy screen and the recovery screen.
- **"Contact support" was intentionally omitted** (decision: no support destination exists in repo).
- Integrity-check failures are logged to `pipeline_trace` with outcome `FAILURE` but never
  auto-quarantine — the next weekly run re-checks.
- Quarantine metadata keys live in `sciuro_db_secure_prefs` (the DB-key store): `quarantine_count`,
  `last_quarantine_timestamp`, `recovery_acknowledged`, `last_integrity_check_ms`,
  `last_integrity_result`.
