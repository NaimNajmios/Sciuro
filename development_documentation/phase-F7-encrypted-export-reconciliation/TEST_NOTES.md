# TEST_NOTES.md — Phase F7: Encrypted Export & Reconciliation Worker

## Retrospective (phase completed prior to G2)

### EncryptedExporter
- [PASS] AES-256-GCM encryption with 128-bit authentication tag
- [PASS] PBKDF2 key derivation (SHA-256, 100,000 iterations)
- [PASS] File format: 4-byte header length + JSON header (magic, version, salt, iv) + ciphertext
- [PASS] Magic bytes "SCIB" for format identification
- [PASS] Version field (1) for future compatibility
- [PASS] SecureRandom for salt (32 bytes) and IV (12 bytes)

### EncryptedImporter
- [PASS] Format validation (header length bounds check, magic "SCIB", version 1)
- [PASS] Base64 salt/IV decoding from JSON header
- [PASS] AES-256-GCM decryption with same parameters as export
- [PASS] Pre-import backup: existing database copied to `sciuro.db.pre_import_backup`
- [PASS] Graceful failure on wrong passphrase (decryption error caught)
- [PASS] Graceful failure on corrupted file (bad header length, unknown version)

### IngestionReconciliationWorker
- [PASS] Counts pending raw events from staging table
- [PASS] Checks notification listener enablement status
- [PASS] Registered twice: 6h from SciuroApp (UPDATE), 15min from MainActivity (KEEP)
- [PASS] 15min worker requires CONNECTED network

### Data Backup Card (Settings)
- [PASS] Info card present in Settings UI describing encrypted-at-rest status
- [PASS] Export/import UI buttons added in Phase G2

### Build Verification
- [PASS] `assembleDebug` — successful
- [PASS] `testDebugUnitTest` — all modules pass
- [MANUAL] Export → delete DB → Import cycle tested with known passphrase
- [MANUAL] Wrong passphrase import tested (correctly fails with generic error)
- [MANUAL] Corrupted file import tested (correctly fails with format error)
