# ADR 020: Database Auto-Backup Exclusion

## Context
Sciuro handles sensitive personal financial data via its internal `core-ledger` SQLDelight databases. By default, Android (API 23+) automatically backs up application data to a user's Google Drive. To prevent unencrypted financial ledgers from syncing to cloud storage inadvertently, we must strictly control what is backed up.

## Decision
1. **Targeted Exclusion**: Rather than setting `android:allowBackup="false"` globally—which would also destroy user preferences and UI state restorations upon app reinstall—we opted for targeted domain exclusions.
2. **Implementation**: 
    - Updated `app/src/main/res/xml/data_extraction_rules.xml` (API 31+) to `<exclude domain="database" path="."/>` from `<cloud-backup>`.
    - Updated `app/src/main/res/xml/backup_rules.xml` (API <31) to `<exclude domain="database" path="."/>` from `<full-backup-content>`.

## Consequences
- The SQDelight database files (`SciuroDatabase.db`) are now strictly local and will not leave the device via Android's Auto Backup mechanism.
- Harmless app configurations (SharedPreferences) can still be safely restored.
- Satisfies security requirement Phase D1.
