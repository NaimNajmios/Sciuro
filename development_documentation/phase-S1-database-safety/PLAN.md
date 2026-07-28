# Phase S1 — Database Safety: Key-Loss Detection, Quarantine, & Backup Key Recovery

## Summary

Three changes to prevent silent data destruction and provide a recovery path for database encryption key-loss events.

### 1. Delete → Quarantine (`PlatformDatabaseModule.kt`)

The old `catch (e: Exception) { context.deleteDatabase("sciuro.db") }` block in the Koin module that creates the `SqlDriver` was destroying the database file when it failed to open with the stored passphrase. The catch caught everything — key-loss, corruption, version mismatch — and all of them triggered an unconditional `deleteDatabase`. Replaced with:

- **Key-loss path**: If the database file exists but `DatabaseKeyManager.passphraseExists()` returns `false`, the file is renamed to `sciuro.db.quarantined.<timestamp>` instead of deleted. A new empty encrypted database is created on next launch.
- **Corruption path**: If the database file exists and a passphrase is stored, but `SQLiteDatabase.openDatabase()` still throws, the file is renamed to `sciuro.db.quarantined.<timestamp>` (and WAL/SHM/journal files are cleaned up). The quarantined file is recoverable by renaming it back.
- Both paths log a warning via `Log.w("SciuroDB", ...)` so the event is not silent.

### 2. Key-loss detection (`DatabaseKeyManager.kt` + `PlatformDatabaseModule.kt`)

The cascade that triggered the data-loss bug: `getOrGeneratePassphrase()` couldn't distinguish "first run (no passphrase yet)" from "key-loss (passphrase existed but is now unreachable)". Added three new methods:

- `passphraseExists(context)`: returns `true` if a passphrase is stored in `EncryptedSharedPreferences`, `false` otherwise (catches `Exception` internally for Keystore unavailability).
- `getStoredPassphrase(context)`: returns the stored passphrase as `ByteArray?`, or `null` if unavailable.
- `storePassphrase(context, passphrase)`: overwrites the stored passphrase with a given value.

The `PlatformDatabaseModule` now runs a **Phase 1** check *before* calling `getOrGeneratePassphrase()`: if the DB file exists but no passphrase is stored, the DB is quarantined immediately. Only then is `getOrGeneratePassphrase()` called (safe to generate a new key since the orphaned DB was already isolated).

Extracted `masterKey()` and `encryptedPrefs()` private helpers in `DatabaseKeyManager` to eliminate duplication across all four methods.

### 3. DB key in encrypted backup (`EncryptedExporter.kt` + `EncryptedImporter.kt`)

Bumped the encrypted backup format to **version 2**: the ciphertext payload now includes the database's SQLCipher passphrase itself (prepended with a 4-byte big-endian length prefix). On import, version 2 backups automatically recover and re-store the DB passphrase via `DatabaseKeyManager.storePassphrase()`, enabling recovery from genuine key-loss events. Version 1 imports remain fully backward-compatible.

## Files changed

| File | Change |
|------|--------|
| `core-ledger/.../security/DatabaseKeyManager.kt` | Added `passphraseExists()`, `getStoredPassphrase()`, `storePassphrase()`. Extracted `masterKey()` / `encryptedPrefs()` private helpers. Changed `toByteArray()` to explicit `Charsets.UTF_8`. |
| `core-ledger/.../di/PlatformDatabaseModule.kt` | Two-phase startup: key-loss detection before passphrase generation, then corruption detection. Both failure paths quarantine instead of deleting. WAL/SHM/journal cleanup on corruption. |
| `app/.../export/EncryptedExporter.kt` | Version 2: reads stored DB key via `DatabaseKeyManager.getStoredPassphrase()`, prepends 4-byte key length + key bytes before DB file bytes in ciphertext. Writes `version: 2` in header. Falls back to version 1 when key unavailable. |
| `app/.../export/EncryptedImporter.kt` | Accepts version 1 or 2. On version 2: extracts DB key from decrypted payload, stores it via `DatabaseKeyManager.storePassphrase()`, writes remaining bytes as DB file. |
