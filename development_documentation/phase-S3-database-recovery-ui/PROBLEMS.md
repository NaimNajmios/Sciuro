# Phase S3 — Database Recovery Interstitial & Weekly Integrity Checks: PROBLEMS

## Concurrent worktree changes collided with this phase

While implementing S3, the working tree changed under the session: the author committed
`feat(audit): implement atomic audit writes and integrity checks` (T1) mid-flight. That commit
touched files this phase also edits:

- `feature-settings/.../DeveloperSettingsViewModel.kt` gained an `auditRepository` constructor
  parameter and an Audit Integrity `StateFlow`.
- `feature-settings/.../di/SettingsModule.kt` already had the extra `get()`.
- `core-ledger` gained `src/jvmTest/.../AuditAtomicityTest.kt`, which references `JdbcSqliteDriver`
  but the module's `jvmTest` source set had no driver dependencies — `:core-ledger:jvmTest` did not
  compile.

Resolution: restored the `auditRepository` constructor param, ordered the new `DatabaseRecoveryManager`
param after `eventBus`, and added the missing `jvmTest` driver dependencies to `core-ledger/build.gradle.kts`.

## Detekt failures are pre-existing, not introduced here

`:core-ledger:detekt` reports two issues in files this phase did not touch:

- `SettingsProvider.kt:3` `ComplexInterface`
- `TransactionRepository.kt:24` `UnusedPrivateProperty` (likely a leftover from the T1 audit refactor)

Both predate S3 and are left untouched to avoid stepping on concurrent work.

## Integrity check cadence on failure

A failing integrity check still advances `last_integrity_check_ms`, so a corrupt database is only
re-detected weekly. This was a deliberate scope choice (failure is logged/traced, never auto-quarantined);
a shorter retry backoff could be revisited in a later phase.
