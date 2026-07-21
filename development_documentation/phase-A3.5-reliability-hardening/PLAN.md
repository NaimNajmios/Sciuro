# Sciuro Notification Parsing Hardening — Implementation Plan

## Overview

This plan restructures the notification ingestion pipeline around one principle: **capture is durable and instantaneous; processing is best-effort and retryable**. It's organized into 4 phases, each producing testable, shippable increments.

---

## Audit Findings (Verified Against Live Codebase)

| Finding | Severity | Verified? | Detail |
|---|---|---|---|
| No `onListenerConnected`/`onListenerDisconnected` | 🔴 1 | Yes | Only `onNotificationPosted` is overridden in `SciuroNotificationService` |
| No `requestRebind()` call | 🔴 1 | Yes | Zero references in the codebase |
| No exception handling in collect block | 🔴 1 | Yes | Only `bookTransaction` is wrapped; parser/account-lookup can crash the collector permanently |
| Direction defaults to INFLOW silently | 🟠 2 | Yes | All 7 rules use `if (isOutflow) OUTFLOW else INFLOW` — no inflow keyword detection |
| No `HttpTimeout`/`HttpRequestRetry` on Ktor | 🟠 2 | Yes | `ParsingModule.kt` HttpClient installs only `ContentNegotiation` |
| RawEvent only in memory (SharedFlow) | 🔴 1 | Yes | `MutableSharedFlow(extraBufferCapacity=100)`, zero references in `core-ledger` |
| Parse failure = silent drop | 🔴 1 | Yes | `println` then `return@collect` — no persistence, no dead-letter |
| All logging is `println` | 🟡 3 | Yes | No structured logging anywhere in the pipeline |
| Amount regex requires exactly 2 decimals | 🟡 3 | Yes | `"""RM\s*([\d,]+\.\d{2})"""` fails on `RM50` or `RM 1,234` |

### Corrections to Initial Audit

1. **Service IS declared in manifest** — in `core-ingestion/src/androidMain/AndroidManifest.xml`, which merges at build time. Adding explicit declaration to app manifest as belt-and-suspenders.
2. **WorkManager IS present** — `app/build.gradle.kts` declares `androidx.work:work-runtime-ktx:2.9.0`, with `ReviewReminderWorker` running every 30 minutes. What's missing is a reconciliation worker for the ingestion pipeline specifically.

---

## Phase 1: Fault Isolation + Direction Bug Fix (No Schema Changes)

**Goal:** Stop one bad notification from killing the pipeline, fix the silent direction misclassification.

### 1.1 — Per-event try/catch in the orchestrator

**File:** `core-classifier/src/commonMain/kotlin/com/sciuro/core/classifier/orchestrator/SciuroIngestionOrchestrator.kt`

Wrap the **entire** body of the `collect { rawEvent -> ... }` block in a `try/catch`. Currently only `bookTransaction` is wrapped. If `parserPipeline.process()` or `accountRepository.getAccountByPackageName()` throws, the whole collector coroutine dies silently.

```kotlin
notificationSource.observeEvents().collect { rawEvent ->
    try {
        // ... existing parsing, triage, account matching, booking ...
    } catch (e: Exception) {
        println("SCIURO_ORCHESTRATOR: Unhandled exception processing event ${rawEvent.id}: ${e.message}")
        e.printStackTrace()
    }
}
```

Additionally, add a restart mechanism — if the collector coroutine dies for any reason, it should restart. Simplest approach: a `launch` + `invokeOnCompletion` handler that relaunches if the job completed exceptionally.

### 1.2 — Direction detection: nullable unknown, not silent default

**Files:** All 7 parser rule files:
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/bank/CimbParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/bank/MaybankParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/bank/BsnParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/ewallet/TngParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/ewallet/GrabPayParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/ewallet/BoostParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/ewallet/ShopeePayParserRule.kt`

Change `StructuredDraft.direction` from `TransactionDirection` to `TransactionDirection?` (nullable). In each rule, restructure direction detection:

```kotlin
// BEFORE (all 7 rules):
val isOutflow = text.contains("deducted") || ...
val direction = if (isOutflow) TransactionDirection.OUTFLOW else TransactionDirection.INFLOW

// AFTER:
val isOutflow = text.contains("deducted") || ...
val isInflow = text.contains("credited") || text.contains("received") || text.contains("masuk") || ...
val direction = when {
    isOutflow -> TransactionDirection.OUTFLOW
    isInflow -> TransactionDirection.INFLOW
    else -> null  // Unknown — forces LLM fallback or Review Inbox
}
```

Add Malay and English inflow keywords: `credited`, `received`, `masuk`, `dikreditkan`, `deposit`, `salary`, `refund`.

### 1.3 — Fix amount regex rigidity

**File:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/util/RegexExtractors.kt`

Current: `"""RM\s*([\d,]+\.\d{2})"""` — requires exactly 2 decimal places.

Extend to: `"""RM\s*([\d,]+(?:\.\d{1,2})?)"""` — optionally matches 0, 1, or 2 decimal places. Normalize in `extractAmount()` to always return a proper Double.

### 1.4 — Multi-factor confidence

**File:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/model/StructuredDraft.kt`

Change `isConfident: Boolean` to `confidenceScore: Float` (0.0–1.0). Each parser rule computes a score based on: amount extracted (+0.3), direction positively detected (+0.3), merchant extracted (+0.2), rule-specific reliability (+0.2). A configurable threshold (default 0.7) in `SettingsProvider` determines whether the deterministic result is accepted or sent to LLM fallback.

Update `SciuroParserPipeline.process()` to compare `confidenceScore` against threshold instead of checking `isConfident == true`.

### 1.5 — Tests for direction bug fix

**Files:** `core-parsing/src/commonTest/kotlin/com/sciuro/core/parsing/fixture/ParserTestCase.kt` and all 7 `*ParserRuleTest.kt` files.

Add test cases for:
- Inflow notifications (credited, received) — should return `INFLOW`
- Ambiguous notifications (no direction keywords) — should return `null` direction
- Notifications with unrecognized phrasing — should NOT default to INFLOW
- Amount with no decimals — should parse successfully

---

## Phase 2: Durable Capture + Service Lifecycle (Schema Change Required)

**Goal:** Raw events are persisted to disk before any processing. Service lifecycle hooks are added.

### 2.1 — Add RawEventStaging table

**New file:** `core-ledger/src/commonMain/sqldelight/com/sciuro/core/ledger/db/RawEventStaging.sq`

```sql
CREATE TABLE raw_event_staging (
    id TEXT NOT NULL PRIMARY KEY,
    source_type TEXT NOT NULL,
    source_package_or_address TEXT NOT NULL,
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    captured_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    processed_at INTEGER
);

insertRawEvent:
INSERT INTO raw_event_staging(id, source_type, source_package_or_address, title, text, timestamp, captured_at, status)
VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING');

markProcessing:
UPDATE raw_event_staging SET status = 'PROCESSING', attempt_count = attempt_count + 1, last_error = ? WHERE id = ?;

markProcessed:
UPDATE raw_event_staging SET status = 'PROCESSED', processed_at = ? WHERE id = ?;

markDeadLetter:
UPDATE raw_event_staging SET status = 'DEAD_LETTER', last_error = ? WHERE id = ?;

selectPendingEvents:
SELECT * FROM raw_event_staging WHERE status = 'PENDING' ORDER BY captured_at ASC;

selectDeadLetterEvents:
SELECT * FROM raw_event_staging WHERE status = 'DEAD_LETTER' ORDER BY captured_at DESC;

selectLastCapturedAt:
SELECT MAX(captured_at) FROM raw_event_staging;

countByStatus:
SELECT status, COUNT(*) as count FROM raw_event_staging GROUP BY status;
```

**New migration file:** `core-ledger/src/commonMain/sqldelight/com/sciuro/core/ledger/db/2.sqm`

```sql
CREATE TABLE raw_event_staging (
    id TEXT NOT NULL PRIMARY KEY,
    source_type TEXT NOT NULL,
    source_package_or_address TEXT NOT NULL,
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    captured_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    processed_at INTEGER
);
```

### 2.2 — RawEventRepository

**New file:** `core-ledger/src/commonMain/kotlin/com/sciuro/core/ledger/repository/RawEventRepository.kt`

```kotlin
class RawEventRepository(private val database: SciuroDatabase) {
    suspend fun persistRawEvent(event: RawEvent) { ... }
    suspend fun markProcessing(id: String, error: String?) { ... }
    suspend fun markProcessed(id: String) { ... }
    suspend fun markDeadLetter(id: String, error: String) { ... }
    fun observePendingEvents(): Flow<List<Raw_event_staging>> { ... }
    fun observeDeadLetterEvents(): Flow<List<Raw_event_staging>> { ... }
    suspend fun getLastCapturedAt(): Long? { ... }
}
```

Register in `ledgerModule` Koin module.

### 2.3 — Persist-first in SciuroNotificationService

**File:** `core-ingestion/src/androidMain/kotlin/com/sciuro/core/ingestion/service/SciuroNotificationService.kt`

Change `onNotificationPosted` to:
1. Filter by allowlist (existing)
2. Write to `raw_event_staging` table **synchronously** via `RawEventRepository.persistRawEvent()`
3. Then emit to SharedFlow as a low-latency wake-up signal (existing)

The SharedFlow becomes an optimization (immediate processing), not the source of truth.

### 2.4 — Orchestrator reads from staging table

**File:** `core-classifier/src/commonMain/kotlin/com/sciuro/core/classifier/orchestrator/SciuroIngestionOrchestrator.kt`

Refactor `startListening()`:
- SharedFlow `collect` triggers processing of PENDING rows from the staging table
- Each row is wrapped in its own try/catch (from Phase 1)
- On success: mark `PROCESSED`
- On failure after retries: mark `DEAD_LETTER`
- Add idempotent booking: before `bookTransaction`, check for existing transaction with same `(sourcePackageOrAddress, amount, timestamp ± 60s)` fingerprint

### 2.5 — Service lifecycle hooks

**File:** `core-ingestion/src/androidMain/kotlin/com/sciuro/core/ingestion/service/SciuroNotificationService.kt`

Add:
- `onListenerConnected()`: call `getActiveNotifications()` and process each through the allowlist + persist-first path (backfill missed notifications)
- `onListenerDisconnected()`: call `requestRebind()` as defensive measure

### 2.6 — App manifest explicit declaration

**File:** `app/src/main/AndroidManifest.xml`

Add inside `<application>`:

```xml
<service
    android:name="com.sciuro.core.ingestion.service.SciuroNotificationService"
    android:label="@string/app_name"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

---

## Phase 3: LLM Hardening + Config Externalization

**Goal:** HTTP resilience, parameterized LLM config, circuit breaker.

### 3.1 — Add HttpTimeout + HttpRequestRetry to Ktor HttpClient

**File:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/di/ParsingModule.kt`

```kotlin
HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 3)
        exponentialDelay()
    }
}
```

Ktor 2.3.9 already supports both plugins — no version bump needed.

### 3.2 — Classify LLM failures

**File:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/engine/LlmFallbackParser.kt`

Replace single `catch (e: Exception)` with:
- `IOException` → network failure, return null (retryable via staging table)
- `HttpResponseException` with 401 → surface "invalid API key" state
- `HttpResponseException` with 429 → respect `Retry-After`, back off
- `SerializationException` → malformed response, return null
- Everything else → log and return null

### 3.3 — Externalize LLM parameters

**New file:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/config/LlmParsingConfig.kt`

```kotlin
data class LlmParsingConfig(
    val modelName: String = "llama-3.1-8b-instant",
    val temperature: Double = 0.0,
    val endpointUrl: String = "https://api.groq.com/openai/v1/chat/completions",
    val requestTimeoutMs: Long = 30_000,
    val maxRetries: Int = 3,
    val circuitBreakerThreshold: Int = 5,
    val cooldownMs: Long = 300_000
)
```

Update `SettingsProvider` interface with `getLlmConfig(): LlmParsingConfig`. Update `LlmFallbackParser` to use config instead of hardcoded literals.

### 3.4 — Circuit breaker

**File:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/engine/LlmFallbackParser.kt`

Track consecutive failure count. After `circuitBreakerThreshold` failures, stop making API calls for `cooldownMs`. Surface the tripped state via a new `isCircuitBroken(): Boolean` method on `SettingsProvider`.

### 3.5 — Add WorkManager reconciliation worker

**New file:** `app/src/main/java/com/najmi/sciuro/worker/IngestionReconciliationWorker.kt`

New `CoroutineWorker` that:
1. Calls `getActiveNotifications()` as backfill safety net
2. Retries PENDING staging rows with backoff
3. Checks notification-listener permission is still granted, surfaces warning if not

Schedule in `SciuroApp.onCreate()` alongside the existing `ReviewReminderWorker`.

### 3.6 — Health signal

Add `getLastCapturedAt()` query result to a settings/developer screen. Simple "Last captured: HH:MM" text that makes silent stalls visible.

---

## Phase 4: OEM Resilience + Config Surface

**Goal:** Malaysia-specific background execution guidance, fully parameterized config.

### 4.1 — Battery optimization exemption request

In the existing onboarding flow, after the notification-listener permission is granted, show a rationale screen explaining why battery optimization exemption is needed, then launch `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

### 4.2 — OEM-specific autostart guidance

**New file:** `app/src/main/java/com/najmi/sciuro/util/OemAutostartHelper.kt`

```kotlin
object OemAutostartHelper {
    fun getAutostartIntent(): Intent? = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> Intent().apply { component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity") }
        "oppo", "realme" -> Intent().apply { component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity") }
        "vivo" -> Intent().apply { component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity") }
        "huawei", "honour" -> Intent().apply { component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity") }
        else -> null
    }

    fun getGuideSteps(): List<String> = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> listOf("Open Settings > Apps > Manage apps", "Find Sciuro", "Enable Autostart", "Go to Battery > Sciuro > No restrictions")
        // ... other OEMs
        else -> listOf("Open your device Settings", "Find Sciuro in the Apps list", "Ensure background activity is allowed", "Disable battery optimization for Sciuro")
    }
}
```

### 4.3 — Dead-letter viewer for Review Inbox

Surface `DEAD_LETTER` staging rows in the developer tools or Kanban board. Each entry shows: raw notification text, source package, capture time, error message. User can manually create a transaction from it or dismiss it.

### 4.4 — Configurable allowlist

Move `IngestionConfig.allowedPackages` from a hardcoded `object` to a `Set<String>` persisted via `SettingsProvider`, seeded from the current hardcoded defaults. Expose an edit UI in Developer Settings.

### 4.5 — Parser health metrics

Query the staging table grouped by `sourcePackageOrAddress` and processing outcome. Surface match-rate per bank in Developer Settings. If a previously-reliable rule's rate drops, show a warning.

---

## Phase Dependencies

```
Phase 1 (fault isolation + direction fix)
    ↓
Phase 2 (durable capture + service lifecycle) — depends on Phase 1's try/catch and direction changes
    ↓
Phase 3 (LLM hardening + config) — depends on Phase 2's staging table for retry/dead-letter
    ↓
Phase 4 (OEM resilience + config surface) — depends on Phase 3's health signal and staging table
```

---

## Testing Additions Per Phase

### Phase 1:
- Add inflow test cases to all 7 parser rule tests
- Add ambiguous-direction test cases (should return null direction)
- Add no-decimal amount test cases
- Add a test that verifies the orchestrator collector survives an exception from the parser (force-throw in a mock parser)

### Phase 2:
- Test that `persistRawEvent` writes a row to the staging table
- Test that a simulated process death mid-processing leaves a `PENDING` row recoverable on restart
- Test idempotent booking (same notification processed twice doesn't create duplicates)
- Test `getActiveNotifications()` backfill path

### Phase 3:
- Test circuit breaker trips after N failures and resets after cooldown
- Test HttpTimeout configuration (mock a slow server, verify timeout fires)
- Test retry behavior (mock a server that fails twice then succeeds)

---

## Configuration Surface

Every tunable introduced by this plan, living behind `SettingsProvider` or config objects it owns:

| Parameter | Current state | Proposed home |
|---|---|---|
| Allowlisted packages | Hardcoded `IngestionConfig` | Persisted, runtime-editable list |
| Aggregator email pre-filter keywords | Hardcoded in `SciuroNotificationService` | Config-driven list |
| Confidence threshold | Implicit boolean | Explicit float threshold |
| LLM model name | Hardcoded string literal | `LlmParsingConfig.modelName` |
| LLM temperature | Hardcoded `0.0` | `LlmParsingConfig.temperature` |
| LLM endpoint URL | Inlined in `post()` call | `LlmParsingConfig.endpointUrl` |
| HTTP timeout / retry | Not configured | `LlmParsingConfig.timeoutMs`, `.maxRetries` |
| Circuit breaker threshold / cooldown | Doesn't exist | `LlmParsingConfig.circuitBreakerThreshold`, `.cooldownMs` |
| WorkManager reconciliation interval | Doesn't exist | `BackgroundReliabilityConfig.reconciliationIntervalMinutes` |
| Parser rule definitions | Hardcoded Kotlin classes | Versioned data files (future) |

---

## Documentation

Create `development_documentation/phase-A3.5-reliability-hardening/` with:
- `PROBLEMS.md` — the verified findings
- `FIXES.md` — what was changed and why
- `FEEDBACK.md` — any issues encountered during implementation
- `TEST_NOTES.md` — test results and coverage

Same pattern for `phase-A3.6-background-resilience/` and `phase-A3.7-parsing-llm-hardening/`.
