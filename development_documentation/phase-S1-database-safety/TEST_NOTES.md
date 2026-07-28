# TEST_NOTES.md — Phase S1: Database Safety

## Verification

### Build
- [PASS] `:app:compileDebugKotlin` + `:core-ledger:compileDebugKotlinAndroid` — BUILD SUCCESSFUL

### DatabaseKeyManager
- [CODE] `passphraseExists()` returns `false` when no `EncryptedSharedPreferences` file exists (fresh install)
- [CODE] `passphraseExists()` returns `true` after `getOrGeneratePassphrase()` has been called
- [CODE] `passphraseExists()` returns `false` on `Exception` (Keystore unavailable, corrupted prefs)
- [CODE] `getStoredPassphrase()` returns `null` when no passphrase stored, returns matching bytes when stored
- [CODE] `storePassphrase()` overwrites stored passphrase, subsequent `getOrGeneratePassphrase()` returns the new value
- [CODE] `storePassphrase()` produces bytes that, when converted to `String` and stored via `putString`, can be read back as identical bytes via `getString().toByteArray()`

### PlatformDatabaseModule (key-loss detection)
- [CODE] Phase 1: if `dbFile.exists() && !passphraseExists()`, file is renamed to `*.quarantined.*`, passphrase is NOT generated first
- [CODE] Phase 2: if `dbFile.exists() && passphraseExists()`, DB is opened with passphrase — on failure, file is quarantined and WAL/SHM/journal cleaned up
- [CODE] Phase 1 executes before `getOrGeneratePassphrase()` (avoids generating a new key before detection)
- [CODE] Normal first-run flow (no DB file) passes through both phases and creates new encrypted DB
- [CODE] Normal existing-DB flow (DB file exists, passphrase exists, open succeeds) works without quarantine

### EncryptedExporter + EncryptedImporter (version 2)
- [CODE] Exporter reads stored DB key, prepends 4-byte length prefix + key bytes before DB file bytes
- [CODE] Exporter writes `"version":2` in header when key is included
- [CODE] Exporter falls back to `"version":1` when no key available (defensive)
- [CODE] Importer accepts version 1 (backward compatible)
- [CODE] Importer accepts version 2, extracts key via 4-byte prefix, stores via `storePassphrase()`, writes remaining bytes as DB
- [CODE] Importer handles malformed key length (<=0 or > payload size) gracefully — treats entire decrypted payload as DB file
- [MANUAL] Export with version 2 → delete DB (simulate corruption) → import → app launches with restored data + recovered key
- [MANUAL] Version 1 backup import unchanged — no key recovery attempted
