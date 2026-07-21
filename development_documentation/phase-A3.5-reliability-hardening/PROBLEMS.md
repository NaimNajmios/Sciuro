# Phase A3.5 — Reliability Hardening: Problems Found

## Verified Against Live Codebase

All findings below cite actual file paths from the clone at `github.com/NaimNajmios/Sciuro`.

### 🔴 Severity 1 — A single bad notification can silently kill all future processing

**File:** `core-classifier/src/commonMain/kotlin/com/sciuro/core/classifier/orchestrator/SciuroIngestionOrchestrator.kt`

The entire pipeline runs inside one `Flow.collect {}` block. Only `bookTransaction` was wrapped in try/catch. If `parserPipeline.process()` or `accountRepository.getAccountByPackageName()` threw, the whole collector coroutine terminated silently. Since `startListening()` guards re-entry with `if (job?.isActive == true) return` and nothing restarts a dead collector, one malformed notification could permanently stop 100% of future capture.

**Fix applied:** Wrapped entire collect body in try/catch. Added `invokeOnCompletion` restart mechanism. Each event runs in its own `processOneEvent()` call.

### 🔴 Severity 1 — Parse failure means total data loss, not degraded service

**File:** `core-classifier/src/commonMain/kotlin/com/sciuro/core/classifier/orchestrator/SciuroIngestionOrchestrator.kt` (pre-fix)

When `parserPipeline.process()` returned null, the code did `println(...); return@collect` — no persistence, no dead-letter entry. This directly violated "no notification goes over the system."

**Fix applied:** Orchestrator now marks staging rows as `DEAD_LETTER` with the error reason when parsing fails or direction is unknown.

### 🔴 Severity 1 — Nothing is durable until after parsing succeeds

`RawEvent` lived only in a `MutableSharedFlow<RawEvent>(extraBufferCapacity = 100)` in `NotificationSourceAdapter`, entirely in process memory. Zero references to `RawEvent` existed anywhere in `core-ledger`. Any process death mid-pipeline lost the notification.

**Fix applied:** Added `raw_event_staging` SQLDelight table. Service persists each raw event to disk synchronously before emitting to the pipeline. The SharedFlow is now a wake-up signal, not the source of truth.

### 🟠 Severity 2 — Direction defaults to a guess, not "unknown"

**Files:** All 7 parser rules:
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/bank/CimbParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/bank/MaybankParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/bank/BsnParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/ewallet/TngParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/ewallet/GrabPayParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/ewallet/BoostParserRule.kt`
- `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/rule/ewallet/ShopeePayParserRule.kt`

Every rule used `if (isOutflow) OUTFLOW else INFLOW` — no inflow keyword detection existed. A transaction with unrecognized phrasing silently defaulted to INFLOW. Combined with `isConfident = merchant != null` (direction confidence wasn't part of the signal), a transaction could be marked confident with an incorrect direction.

**Fix applied:** Added positive inflow keyword detection (`credited`, `received`, `masuk`, `dikreditkan`, `deposit`, `refund`, `top-up`). Direction is now nullable — null means "unknown," forcing the event to LLM fallback or dead-letter. Replaced `isConfident: Boolean` with `confidenceScore: Float` (0.0–1.0) combining amount, direction, and merchant factors.

### 🟠 Severity 2 — LLM fallback has no timeout, no retry, everything is hardcoded

**File:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/di/ParsingModule.kt`

Shared HttpClient installed only `ContentNegotiation` — no `HttpTimeout`, no `HttpRequestRetry`. A slow or hung Groq response could stall indefinitely.

**File:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/engine/LlmFallbackParser.kt` (pre-fix)

Model name (`"llama-3.1-8b-instant"`), temperature (`0.0`), and endpoint URL were hardcoded literals. The blanket `catch (e: Exception)` treated all failures identically.

**Fix applied:** Installed `HttpTimeout` (30s request, 10s connect) and `HttpRequestRetry` (3 retries, exponential backoff). Externalized all parameters to `LlmParsingConfig` data class with sensible defaults. Added failure classification (timeout, network, serialization, API error, unexpected). Added circuit breaker — after 5 consecutive failures, stops calling for 5 minutes.

### 🟠 Severity 2 — No background-execution resilience

No `onListenerConnected`/`onListenerDisconnected` overrides. No `requestRebind()` call. No reconciliation worker for the ingestion pipeline specifically.

**Fix applied:** Added `onListenerConnected()` with `getActiveNotifications()` backfill. Added `onListenerDisconnected()` with `requestRebind()`. Created `IngestionReconciliationWorker` (15-min periodic via WorkManager) that retries PENDING staging rows.

### 🟡 Severity 3 — Amount regex requires exactly 2 decimal places

**File:** `core-parsing/src/commonMain/kotlin/com/sciuro/core/parsing/util/RegexExtractors.kt`

`"""RM\s*([\d,]+\.\d{2})"""` — fails on `RM50` or `RM 1,234`.

**Fix applied:** `"""RM\s*([\d,]+(?:\.\d{1,2})?)"""` — optionally matches 0–2 decimal places. Normalized in `extractAmount()`.

### 🟡 Severity 3 — Everything is println, nothing is queryable

All pipeline logging goes to stdout. No structured persistence of what happened with a given notification.

**Fix applied:** Staging table now records status transitions (PENDING → PROCESSING → PROCESSED/DEAD_LETTER) with attempt count and last error messages.

### Corrections from Original Audit

1. **Service IS declared in manifest** — in `core-ingestion/src/androidMain/AndroidManifest.xml`, which merges at build time. Added belt-and-suspenders declaration to app manifest.
2. **WorkManager IS present** — `app/build.gradle.kts` declares `androidx.work:work-runtime-ktx:2.9.0` with `ReviewReminderWorker` running every 30 minutes. What was missing was a reconciliation worker for the ingestion pipeline specifically — now added.
