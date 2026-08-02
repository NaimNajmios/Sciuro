# TEST_NOTES.md — Phase T1: Atomic Audit Writes & Audit Integrity

## Problem

`AuditableRepository.withAudit()` executed the mutation, then wrote the audit log as a separate
auto-committed operation. A process death between the two left a committed-but-unaudited mutation —
a direct violation of the README claim "every data mutation passes through a unified Audit Log,
ensuring complete traceability."

## Root cause

SQLDelight's `database.transaction { }` / `transactionWithResult { }` lambdas are non-suspending in
KMP `commonMain`, so the suspend `auditRepository.logMutation()` could not run inside them
(previously documented in `phase-H1-pipeline-tracing/TEST_NOTES.md` as DEFERRED).

## Change

- `AuditRepository.logMutation()` is now synchronous. `SqlDelightAuditRepository` implements it with
  the same `auditLogQueries.insertLog(...)` call, now callable inside a transaction lambda.
- All SQLDelight-backed mutation paths wrap the mutation AND the audit insert in the same
  `database.transaction { }` (or `transactionWithResult { }` for value-returning methods).
- `withAudit` removed from `AuditableRepository` (it was the non-atomic wrapper). `AuditableRepository`
  remains the shared `protected auditRepository` holder.
- `CashAdjustmentRepository` and `ReconciliationEngine` now call `database.accountQueries.updateBalance()`
  directly inside the transaction instead of `AccountRepository.updateBalance()` (which carried its own
  audit wrapper). `AccountRepository.updateBalance()` removed (no remaining callers).
- `CategoryRepository.updateCategory()` / `deleteCategory()` were previously **not audited at all** —
  audit entries added.
- Added `auditIntegrityCheck` query + `AuditRepository.getAuditIntegrityGaps()`.
- Developer Options → Health tab shows the missing-audit count (green 0 / danger-red > 0).

## Verification

- [PASS] `:core-ledger:jvmTest` — `AuditAtomicityTest` (4 tests), BUILD SUCCESSFUL:
  1. `bookTransaction` commits mutation + audit + balance atomically; `getAuditIntegrityGaps() == 0`.
  2. Audit-write failure rolls back the transaction insert AND the balance update.
  3. `auditIntegrityCheck` counts an orphaned (unaudited) transaction.
  4. Clean state reports zero gaps, including after `deleteTransaction` (DELETE audit kept).
- [PASS] `:core-transfer:jvmTest` — completed without failures in the combined run (TransferRepository rewrite + updated test fakes).
- [COMPILE] All changed sources compile: `core-audit` (JVM + Android), `core-ledger` (JVM main), `core-obligations` main, `core-debt`, `core-investment`, `core-budget`, `core-transfer` (main + test), `:feature-settings:compileDebugKotlinAndroid`, `:app:compileDebugKotlin`.
- [BLOCKED-pre-existing] `:core-obligations:compileTestKotlinJvm` fails on `ObligationDetectionEngineTest.TestSettingsProvider` not implementing the new `SettingsProvider.hasCompletedOnboarding()` member — added by in-flight working-tree changes, unrelated to this phase. `ObligationRepository` main sources compile cleanly.
- [NOT-RERUN] `:core-classifier:testDebugUnitTest` and the remaining Android test tasks were not re-run after the combined run was interrupted by the pre-existing obligations test failure; test fakes in `core-classifier` were updated for the synchronous `logMutation` and the module's main/test compile was reached in the combined run.
