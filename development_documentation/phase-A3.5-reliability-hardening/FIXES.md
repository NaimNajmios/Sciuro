# Phase A3.5 — Reliability Hardening: Changes Made

## Files Modified

### `core-classifier/`
| File | Change |
|---|---|
| `src/commonMain/kotlin/com/sciuro/core/classifier/orchestrator/SciuroIngestionOrchestrator.kt` | Wrapped entire `collect` body in try/catch. Added `invokeOnCompletion` restart mechanism. Added `RawEventRepository` dependency for staging lifecycle. Per-event processing in `processOneEvent()`. Mark events as PROCESSING/PROCESSED/DEAD_LETTER. Handle nullable direction gracefully. |
| `src/commonMain/kotlin/com/sciuro/core/classifier/di/ClassifierModule.kt` | Added `rawEventRepository = get()` to orchestrator DI wiring. |

### `core-parsing/`
| File | Change |
|---|---|
| `src/commonMain/kotlin/com/sciuro/core/parsing/model/StructuredDraft.kt` | `direction` changed from `TransactionDirection` to `TransactionDirection?`. `isConfident: Boolean` replaced with `confidenceScore: Float`. Added `DEFAULT_CONFIDENCE_THRESHOLD` constant. | 
| `src/commonMain/kotlin/com/sciuro/core/parsing/util/RegexExtractors.kt` | Amount regex extended to support 0–2 decimal places. Merchant regex split into `outflowMerchantRegex` and `inflowMerchantRegex` (adds `from`/`dari` for inflow contexts). |
| `src/commonMain/kotlin/com/sciuro/core/parsing/engine/SciuroParserPipeline.kt` | Uses `confidenceScore >= confidenceThreshold` instead of `isConfident`. `confidenceThreshold` is configurable with default. |
| `src/commonMain/kotlin/com/sciuro/core/parsing/engine/LlmFallbackParser.kt` | Failure classification (timeout, network, serialization, API error, unexpected). Circuit breaker (5 consecutive failures → 5min cooldown). All parameters externalized via `LlmParsingConfig`. |
| `src/commonMain/kotlin/com/sciuro/core/parsing/config/LlmParsingConfig.kt` | **NEW** — Data class for model name, temperature, endpoint URL, timeouts, retries, circuit breaker thresholds. |
| `src/commonMain/kotlin/com/sciuro/core/parsing/config/SettingsProvider.kt` | Added `getLlmConfig(): LlmParsingConfig` with default implementation. |
| `src/commonMain/kotlin/com/sciuro/core/parsing/di/ParsingModule.kt` | Installed `HttpTimeout` (30s request, 10s connect) and `HttpRequestRetry` (3 retries, exponential delay) on shared HttpClient. Injects `LlmParsingConfig` into `LlmFallbackParser`. |

### All 7 Parser Rules
| File | Change |
|---|---|
| `CimbParserRule.kt` | Added inflow keyword detection (`credited`, `received`, `masuk`, `dikreditkan`). Direction nullable. Confidence score computation. |
| `MaybankParserRule.kt` | Same pattern as CIMB + `deposit`. |
| `BsnParserRule.kt` | Same pattern + removed misleading "BSN generally means outflow" comment. |
| `TngParserRule.kt` | Added inflow (`received`, `top-up`, `credited`, `masuk`). |
| `GrabPayParserRule.kt` | Added inflow (`received`, `credited`, `top-up`). |
| `BoostParserRule.kt` | Added inflow (`received`, `credited`, `top-up`). |
| `ShopeePayParserRule.kt` | Added inflow (`received`, `credited`, `refund`). |

### `core-ingestion/`
| File | Change |
|---|---|
| `build.gradle.kts` | Added `implementation(project(":core-ledger"))` dependency. |
| `src/androidMain/kotlin/com/sciuro/core/ingestion/service/SciuroNotificationService.kt` | Added `onListenerConnected()` with `getActiveNotifications()` backfill. Added `onListenerDisconnected()` with `requestRebind()`. Persist-first in `processAndPersistNotification()` — writes to staging table before emitting. |

### `core-ledger/`
| File | Change |
|---|---|
| `src/commonMain/sqldelight/com/sciuro/core/ledger/db/RawEventStaging.sq` | **NEW** — Table: `raw_event_staging` with columns for id, source info, text, status, attempt count, error tracking. Queries for insert, status transitions, pending/dead-letter listing, health metrics. |
| `src/commonMain/sqldelight/com/sciuro/core/ledger/db/2.sqm` | **NEW** — Migration: creates `raw_event_staging` table. |
| `src/commonMain/kotlin/com/sciuro/core/ledger/repository/RawEventRepository.kt` | **NEW** — Repository for staging table operations: persist, mark status transitions, observe pending/dead-letter rows, last-captured-at health signal. |
| `src/commonMain/kotlin/com/sciuro/core/ledger/di/LedgerModule.kt` | Registered `RawEventRepository` as Koin singleton. |

### `app/`
| File | Change |
|---|---|
| `src/main/AndroidManifest.xml` | Added explicit `<service>` declaration for `SciuroNotificationService` with `BIND_NOTIFICATION_LISTENER_SERVICE` permission. |
| `src/main/java/com/najmi/sciuro/MainActivity.kt` | Added `IngestionReconciliationWorker` scheduling (15-min periodic, requires network). Added `Constraints`/`NetworkType` imports. |
| `src/main/java/com/najmi/sciuro/worker/IngestionReconciliationWorker.kt` | **NEW** — `CoroutineWorker` that retries PENDING staging events on schedule. |

### Tests
| File | Change |
|---|---|
| `core-parsing/src/commonTest/.../ParserTestCase.kt` | `expectedDirection` now nullable `TransactionDirection?`. Added `expectNull` flag. |
| All 7 `*ParserRuleTest.kt` files | Added inflow test cases. Added ambiguous-direction test cases (expect null direction). |

## Build Verification

- `./gradlew :core-parsing:compileDebugKotlinAndroid` — passes
- `./gradlew :core-ingestion:compileDebugKotlinAndroid` — passes
- `./gradlew :core-classifier:compileDebugKotlinAndroid` — passes
- `./gradlew :core-ledger:compileDebugKotlinAndroid` — passes
- `./gradlew :app:compileDebugKotlin` — passes
- `./gradlew :core-parsing:testDebugUnitTest` — all 7 tests pass (7 test classes, 7 test methods, 0 failures)
