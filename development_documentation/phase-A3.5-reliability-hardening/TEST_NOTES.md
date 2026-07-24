# TEST_NOTES.md — Phase A3.5: Reliability Hardening

## Retrospective (phase completed prior to G2)

This phase was implemented and verified during earlier development. Tests were primarily manual and integration-level, supplemented by build verification.

### Fault Isolation
- [PASS] Per-event try/catch wrapping prevents single bad notification from killing the orchestrator
- [PASS] Automatic restart on collector failure (`invokeOnCompletion` handler)
- [PASS] Dead-letter marking on parse failures, direction unknown, and unhandled exceptions

### Durable Capture
- [PASS] `RawEventStaging` table persists events before pipeline processing
- [PASS] `SciuroNotificationService` persists to staging table before emitting to adapter
- [PASS] Status transitions: PENDING → PROCESSING → PROCESSED / DEAD_LETTER

### LLM Hardening
- [PASS] HTTP timeout (30s connect, 60s read)
- [PASS] Retry logic with exponential backoff (3 attempts)
- [PASS] Circuit breaker (5 consecutive failures → 5min cooldown)
- [PASS] Externalized LLM config via `SettingsProvider`
- [PASS] Groq API key test endpoint in Settings UI

### OEM Resilience
- [PASS] `OemAutostartHelper` with manufacturer-specific intents and guide steps
- [PASS] `IngestionReconciliationWorker` (6h periodic, UPDATE policy)
- [PASS] Notification listener rebinding on disconnect
- [PASS] Explicit manifest service declaration with `BIND_NOTIFICATION_LISTENER_SERVICE`

### Direction Fix
- [PASS] Direction is now nullable in parser drafts
- [PASS] Inflow keyword detection (received, credited, transferred in, cashback, refund)
- [PASS] Confidence scoring with amount-based confidence boost (>RM 5.0)
- [PASS] Fix verified via `SimulationEngine` diagnostics in Developer Settings

### Build Verification
- [PASS] `assembleDebug` — successful
- [PASS] `testDebugUnitTest` — all modules pass
- [MANUAL] OEM autostart intents tested on Xiaomi (Redmi), Oppo (ColorOS), Samsung (One UI)
- [MANUAL] Notification listener rebinding tested by force-stopping app and re-launching
