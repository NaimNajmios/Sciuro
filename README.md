# Sciuro 🐿️

Sciuro is an advanced, privacy-first personal finance and asset management application built with Kotlin Multiplatform (KMP). It is designed with rigorous engineering standards to provide full auditability, intelligent automated tracking, and multi-source financial ingestion.

## Key Features

* **Deterministic Self-Transfer Detection:** Two-tier matching engine identifies cross-account transfers using counterparty account numbers extracted from all 7 bank/ewallet notification parsers (Tier 1), falling back to amount+time heuristic only when no account number is present (Tier 2). Masked-number suffix matching handles partially-hidden account numbers (e.g., "...7890", "****7890"). Human-confirmed pairs auto-link on future matches.
* **Account Data Enrichment:** Each account stores its own account number, account holder name, bank code, and QR code image. These fields are set once via the Account Detail edit sheet and enable identity-based transfer matching instead of coincidence-based guessing.
* **QR Code Display:** Bank accounts and e-wallets can store a QR code image (captured from gallery) for quick display when receiving payments. A QR icon button in the account detail hero section opens a large full-screen dialog for scannable display. Cash wallets (Personal Wallet) do not expose QR code features.
* **Universal Operations Confirmation:** Destructive data mutation actions (deleting transactions, budgets, accounts, archiving) are guarded by standardized confirmation pop-ups requiring explicit user acknowledgement. Non-destructive actions (create, save) execute immediately with snackbar feedback.
* **Obligation / Recurring Bill Tracking:** Pattern-based auto-detection of subscriptions and recurring outflows. Per-transaction cycle matching that automatically advances due dates when a matching payment is booked. Features configurable auto-confirmation thresholds for highly confident matches and silent auto-confirm for trusted amounts. Bills can be created, edited, deleted, or deactivated manually.
* **Debt Tracking:** Full CRUD for debts with direction support (I Owe / Owed to Me), progress tracking, counterparty identification, and lifecycle management (Active / Paid Off / Finished / Archived). Automatic payment matching via `DebtEngine` respects direction — repayments owed to you are correctly recognized from incoming transactions. Allows manual status overriding.
* **Budget Tracking:** Full CRUD for category budgets with per-category spending limits, progress bars, and reactive spend recalculation. Three-state visual (Healthy / Approaching / Over) with per-budget alert thresholds backed by a configurable global slider. Calendar-month boundaries for MONTHLY budgets. Transfer-linked transactions are excluded from spend calculations. Optional rollover carries unused budget to the next period. Budget cards display category icons, period badges (Weekly/Monthly/Yearly), daily allowance (RM/day), and days remaining in the current period. Category drilldown screen provides per-category spend analysis with back navigation. Create/edit/delete budgets via bottom sheet with category picker, period selector, inline amount validation, and snackbar feedback on success. `BudgetLimitSuggester` provides 90-day trimmed-mean spend suggestions on budget creation. HeroPanel shows total spent vs allocated with at-risk budget highlights and a "View Categories" navigation link.
* **Malaysian Payment Channels:** Deep integration and detection rules for local payment platforms, physical wallets, and e-wallets. Features 11 dedicated bank/e-wallet notification parsers including Maybank, CIMB, RHB, Hong Leong, Public Bank, HSBC, OCBC, UOB, GxBank, Aeon, and Standard Chartered.
* **Investment & Gold Savings:** Native support for tracking complex assets like gold and long-term investments.
* **Audit-First Architecture:** Every data mutation passes through a unified Audit Log, ensuring complete traceability.
* **Kanban Workflow:** A unified task management and issue tracking system deeply integrated into the development process.
* **Guided Onboarding:** Initial setup flow securely initializes system ledgers with physical cash on hand.
* **Fast Logging Workflow:** Calculator-first, numpad-driven transaction entry screens with pre-filled category pills and descriptions for near-instant offline transaction recording.
* **Interactive Dashboard:** Main dashboard equipped with a customizable Date Range picker to filter displayed transactions over specific periods.
* **UI Standardization & Theming:** Unified design system utilizing custom wrappers (`SciuroBottomSheet`, `SciuroTextField` with inline validation + placeholder + error state, `SciuroCard`, `SciuroPrimaryButton`) across all feature modules. Centralized semantic color tokens (`SignalIncome`, `SignalDanger`, `AccountColor*` presets). Robust 3-way persistent Appearance toggling (Light, Dark, System default). Full accessibility pass: reduced-motion awareness, 44dp touch targets, TalkBack semantics on HeroPanel + PillToggle + confidence indicators + swipe actions. All UI strings externalized to `strings.xml` (~200+ resources across 7 modules). `@Preview` composables for all 7 screens. Animated hero figure count-up and chart sparkline with value label.
* **Interactive UI Triage:** Swipe-to-dismiss capabilities for fast transaction approvals and dynamically updated swipeable wallet interfaces to track cash and investments.
* **User-Configurable Notifications:** 13 notification types across 8 Android channels, each with individual enable/disable toggles and adjustable parameters from Settings > Notifications. Channels: Backup Reminders (configurable interval, 1-30 days), Runway Alerts, Large Transaction alerts (configurable threshold, RM 100-5000), Unusual Spending, Debt Due reminders (configurable days-before, 1-30), Income Not Arrived, Transaction Review reminders, Bill Autopay Confirmed, Weekly Digest (Sundays), Net Worth Milestones (RM 1k+), BNPL Risk, Cash Anomalies, and Transfer Review. All notification preferences are persisted via `EncryptedSharedPreferences` and checked before every notification fires. Background WorkManager integration for periodic checks (daily: backup, runway, debts, income, digest). Quiet hours suppression with runway-aware critical bypass.
* **App Lock:** Optional biometric/PIN gate that secures the app on launch. Re-prompts after 30 seconds of backgrounding. Falls back to a "Set up device security" prompt when no screen lock is enrolled — no silent bypass. Toggled via Settings > Security.
* **SMS Ingestion:** `SmsSourceAdapter` + `SmsReceiver` capture financial SMS messages and route them through the same ingestion pipeline as notifications. Financial signal detection uses a rigorous, testable `AggregatorHeuristicFilter` that filters out non-relevant messages based on regex boundary words and Malaysian financial keywords (including DuitNow, FPX). Supported SMS packages include Google and Samsung messaging apps.
* **New Finance App Detection:** `FinanceAppInstallReceiver` triggers on new package installs, cross-references a catalog of 52 Malaysian finance apps, and surfaces a one-tap "Track it?" notification to add the app to the allowlist.
* **Background Reliability:** One-time battery optimization exemption step with OEM-specific autostart guides (Xiaomi, Oppo/Realme, Vivo, Huawei/Honor). Prompt is tracked persistently via `EncryptedSettingsProvider` to ensure it only asks once. Settings card shows live exemption status with fix actions.
* **Anomaly-Aware Cash Adjustments:** When a balance correction exceeds RM 50 in variance, the bottom sheet prompts for an optional remark to document the discrepancy. Remarks are stored alongside adjustments for future reconciliation reference.
* **Quiet Hours & Runway-Aware Notifications:** Configurable quiet hours window suppresses non-critical budget alerts during off-hours. Negative runway (bills exceeding available funds) bypasses suppression to ensure critical alerts always surface.
* **Configurable Allowlist:** Runtime-editable notification allowlist via Settings > Developer Options > Sources. Add/remove packages dynamically without code changes.

## Project Status

The project is fully functional and has completed **Phase K1 (UI Polish & Accessibility Pass)**, **Phase R1 (Reliability & Performance Hardening)**, and **Phase N2 (User-Configurable Notification Preferences)** addressing cross-cutting architectural concerns.

**Phase R1 highlights:**
- **Atomic database transactions:** All transaction mutations now wrap insert + balance update in SQLDelight `database.transaction { }`, preventing orphaned transactions on crash.
- **SQL-optimized budget engine:** `BudgetEngine` uses indexed `SUM` queries per category+time range instead of loading all transactions into memory.
- **Engine debouncing:** Full-scan engines (budget, debt, investment, obligation detection) debounced with 15s cooldown, eliminating 4× full table scans per ingested event.
- **SMS receiver lifecycle fix:** `SmsReceiver` now properly calls `goAsync()` + `pendingResult.finish()`. Leaked `CoroutineScope` replaced with per-call scoping.
- **Tier 0 transfer detection:** Confirmed account pairs checked before suffix matching.
- **Navigation extraction:** Sealed `SciuroRoute` class + extracted `SciuroNavGraph` composable; `MainActivity` reduced from 522 to ~340 lines.
- **ProGuard rules:** Comprehensive rules for SQLCipher, SQLDelight, Koin, Ktor, kotlinx.serialization.

Two follow-up capture-layer defect fixes in the notification ingestion pipeline resolved three real-world gaps: (1) Maybank2u Scan & Pay and Gmail forwards were being dropped because `SciuroNotificationService` only read `EXTRA_TEXT` (the collapsed preview) while the actual content lived in `EXTRA_BIG_TEXT` or `EXTRA_TEXT_LINES`. (2) Maybank2u Scan & Pay notifications that post with all three standard fields blank and the real content in a custom extra key `full_desc` are now rescued by a two-tier fallback (known package→key mappings, then generic financial-keyword-gated scan). (3) The merchant-name regex was fixed to not truncate abbreviation periods (e.g. "I.R" → "I") by requiring the terminating period to be followed by whitespace or end-of-string. Text extraction reads all three standard fields with a `bigText > textLines > shortText` priority chain, then falls back to custom extra scanning if still blank. The `blank_content` drop path logs which extras keys were present to make future diagnostic reads instant. The `SmsReceiver` was consolidated to use the shared `AggregatorHeuristicFilter` (previously it had its own independently-maintained keyword check). Core domain modules are wired into the ingestion orchestrator and reactive UI. A Domain Event Bus with 23 event types provides cross-module event-driven communication — all published events now have notification subscribers. The Kanban screen unifies transaction review, bill tracking, and debt overview. Live investment pricing (gold spot, Malaysian stocks) is available via `YahooFinancePriceProvider`. Encrypted export/import is accessible from Settings > Data Backup. BNPL/pay-later risk detection, obligation amount drift tracking, cash recount domain events, and credit card statement tracking are all active. A category spending drilldown screen provides per-category budget analysis with unified threshold from Settings and back navigation. Budget cards display category icons, period badges, daily allowance, and days remaining. The budget module has been polished with snackbar feedback, inline validation, direct-action create/save flows, and consistent theme token usage. The design system is fully standardized — `SciuroTextField` is the single text-input surface across all 8 modules (35 call sites migrated), with built-in inline validation, placeholder, and error state support. Accessibility is hardened: reduced-motion gates on all infinite animations, 44dp min touch targets, semantic labels on hero panels, confidence indicators, and swipe actions, and `PillToggle` now emits proper `Role.Tab` semantics with selection state for screen readers. All UI strings are extracted to `strings.xml` resources (~200+ strings across 7 modules), eliminating hardcoded text from Composables. `BudgetLimitSuggester` has 8 JVM unit tests covering trimmed mean, outlier trimming, lookback filtering, and event publication. The Developer Options harness now features 7 tabs with full error handling, two-step delete confirmation, determinate batch progress, Koin dependency cleanup, and a gated activation pattern. `@Preview` composables exist for all 7 feature screens. All core modules — including the multi-source ingestion engine, automated budget tracking with full CRUD, Kanban workflow, and UI feature modules — are fully integrated and tested.

## Architecture

Sciuro is built using a strict modular Kotlin Multiplatform structure:
* **Core Modules** (`core-*`): Reusable domain layers and intelligence engines:
  - `:core-ledger`, `:core-audit`: Foundational persistence and traceability.
  - `:core-ingestion`, `:core-parsing`, `:core-llm`: Notification extraction and LLM fallback parsing. `SciuroNotificationService` reads all three notification extras fields (`EXTRA_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_TEXT_LINES`) with a `bigText > textLines > shortText` priority chain via `NotificationTextResolver` in `commonMain`, then falls back to a two-tier custom extras scan (known package→key mappings like `com.maybank2u.life → full_desc`, then a generic financial-keyword-gated scan) when standard fields are blank. Includes a `PiiScrubber` utility to automatically redact sensitive account/NRIC data prior to LLM processing. Implements intelligent fallback mechanics to filter out self-referential phrases (e.g. "your account") from regex-extracted merchants, and uses lookahead-anchored period termination (`\.(?=\s|$)`) in merchant regexes to prevent abbreviation-period truncation.
  - `:core-classifier`: The central Orchestrator that triages parsed data and triggers transfer detection. It features a fully concurrent coroutine-based ingestion pipeline for high-throughput batch processing.
  - `:core-obligations`, `:core-transfer`, `:core-debt`, `:core-investment`, `:core-budget`: Specialized intelligence engines that track assets, liabilities, recurring expenses, budget thresholds, and identity-based transfer matching.
* **Feature Modules** (`feature-*`): User-facing capabilities: `:feature-dashboard`, `:feature-wallet`, `:feature-budgets`, `:feature-debt`, `:feature-kanban`, `:feature-settings`.

**Tech Stack:**
* **Dependency Injection:** [Koin](https://insert-koin.io/)
* **Local Persistence:** [SQLDelight](https://cashapp.github.io/sqldelight/) + SQLCipher (database encryption)
* **Security:** [AndroidX Biometric](https://developer.android.com/jetpack/androidx/releases/biometric), [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
* **Static Analysis:** [Detekt](https://detekt.dev/)
* **Background Tasks:** WorkManager (Android)
* **UI Framework:** Jetpack Compose (Android)

### HeroPanel — Shared Hero Section Component

`HeroPanel` (`core-ui`) is a dark-backed hero header composable used across 8 screens. The top row uses `SpaceBetween` to place the title on the left and an optional `PillToggle` on the right. A second row renders the large hero figure at full width below. An optional `WaveChart` sparkline and a `content` slot complete the section:

| Screen | heroFigure | chartData | toggle | content slot |
|---|---|---|---|---|
| Dashboard | Total net position | Real daily balance history | This Month / All Time | Accounts count + weekly adjustments |
| Budgets | Total spent vs allocated | — | — | Top 3 at-risk budgets |
| Account Detail | Account balance | — | — | Adjust Balance + QR icon |
| Debt Overview | I Owe / Owed to Me totals | — | — | Direction breakdown row |
| Kanban | Active tasks / Bills due / Active debts (tab-aware) | — | — | Tab-aware: status breakdown / bill urgency / debt totals |
| Developer Settings | Time since last capture | — | — | Pipeline pending/dead counts (7 tabs: Simulator, Sources, Ingestion Log, Diagnostics, Data Tools, Health, Pipeline Trace) |
| Settings (×2) | "Config" / "More" | — | — | — |

### Full-Screen Swiping Architecture

All scrollable screens (Dashboard, Kanban, Budgets, Wallet) follow a consistent full-screen swiping pattern. The root layout uses a `Box` containing a single `LazyColumn` with two `item { }` blocks:

```
Box(fillMaxSize) {
    LazyColumn {
        item { HeroPanel(...) }
        item {
            SheetList(offset(-24.dp).fillParentMaxHeight()) {
                Column { forEach { ... } }
            }
        }
    }
    FAB(align = BottomEnd)
}
```

- The `HeroPanel` scrolls off-screen naturally as the user swipes up.
- The `SheetList` uses `fillParentMaxHeight()` to fill the remaining viewport, with a `-24.dp` offset for the overlapping visual effect.
- Content inside `SheetList` uses plain `Column` + `forEach` (never a nested `LazyColumn`) since the parent `LazyColumn` handles all vertical scrolling.
- The FAB is overlaid in the root `Box` with `Modifier.align(Alignment.BottomEnd)`.
- Hero figure text uses `headlineLarge` typography to prevent number overflow on large figures.

## Developer Tools

Sciuro includes a full developer settings harness at `feature-settings` > `DeveloperSettingsScreen` with seven tabs. Developer Options are hidden by default in the Settings screen and revealed via a hidden activation (tap the "Developer Options" section header 7 times).

| Tab | Description |
|---|---|
| **Simulator** | Manual pipeline: enter package/title/text and run through all parser rules. Includes a dynamic package+template picker sourced from `FixtureLibrary` (over 100 realistic Malaysian bank and e-wallet fixtures across 7 rules). Determinate batch progress bar shows `"12/47: com.dbs.card"` with fractional progress indicator. |
| **Sources** | Editable allowlist view of notification packages grouped by Bank / E-Wallet / Aggregator / Custom. Add/remove packages dynamically — changes take effect immediately for the notification listener. |
| **Ingestion Log** | Dead-letter event viewer with pending/dead-letter counts, per-event error display. Tapping a dead letter opens a form to structurally edit the raw JSON payload and requeue it. |
| **Diagnostics** | Per-rule match/no-match analysis with extracted fields. Shows LLM debug info (prompt, response, latency) when LLM fallback is triggered. |
| **Data Tools** | Clear Inbox (unreviewed transactions) with two-step confirmation dialog requiring "DELETE" text input. |
| **Health** | Per-package parser match-rate monitoring. Track which financial apps provide reliable extraction. Pipeline metrics with LLM fallback counts and dead-letter counts. |
| **Pipeline Trace** | Trace event viewer showing the 100 most recent pipeline events with stage-by-stage breakdown (SUCCESS/FAILURE/DROP), duration, confidence, and detail JSON. Sessions prominently display their package name (e.g. `com.whatsapp`) and capture early drops. Filterable by package name, outcome status, and an "Allowlisted" toggle that hides non-allowlisted app events by default. |

### Developer Options Access

Developer Options are gated behind a hidden activation to prevent non-technical users from accidentally modifying pipeline settings:

1. Navigate to **Settings** screen
2. Scroll to the bottom section labeled "Developer Options"
3. Tap the "Developer Options" section header **7 times** in quick succession
4. A snackbar confirms "Developer Options enabled"
5. The Developer Options card becomes visible and navigable

Once revealed, Developer Options remain visible across app restarts (persisted via `EncryptedSharedPreferences`).

### Error Handling

All Developer Options operations (simulation, batch runs, inbox clearing, dead-letter resends) are wrapped in error handling with Snackbar feedback. Errors are displayed automatically and cleared after acknowledgment.

### Deep Link Support

The `FinanceAppSuggestionSubscriber` notification deep-links directly to the Sources tab of Developer Options when a new finance app is detected, allowing quick add-to-allowlist workflow.

### SMS Ingestion

SMS-based financial notifications can be captured via the `SmsReceiver` (registered for `SMS_RECEIVED`). The receiver:
- Filters by allowlist (sender phone number)
- Detects financial signals in message body via the shared `AggregatorHeuristicFilter.isFinancial()` — same keyword set and word-boundary regex used by `SciuroNotificationService` for aggregator content, ensuring consistent financial signal detection across both notification and SMS channels
- Persists to the `RawEventStaging` table and routes through the same ingestion pipeline as notifications
- Requires `RECEIVE_SMS` permission (declared in manifest)

**Key classes:**
- `SimulationEngine` (`core-parsing`) — runs the full parser pipeline and captures per-rule results, LLM latency, and debug info in a `SimulationResult`.
- `FixtureLibrary` (`core-parsing`) — shared fixture data (over 100 cases) used by both tests and the simulator UI.
- `SimulationResult` / `RuleMatchResult` / `LlmDebugInfo` — data classes for diagnostic output.

## Development Setup

1. **Requirements:**
   - Android Studio Jellyfish (or newer)
   - JDK 17
   - Kotlin 1.9.x

2. **Building the Project:**
   ```bash
   ./gradlew build
   ```

3. **Running Parser Tests:**
   ```bash
   ./gradlew :core-parsing:testDebugUnitTest
   ```

4. **Running Transfer Detection Tests:**
   ```bash
   ./gradlew :core-transfer:jvmTest
   ```

5. **Running All Tests:**
   ```bash
   ./gradlew allTests
   ```

6. **Running Static Analysis (Detekt):**
   ```bash
   ./gradlew detekt
   ```

## Documentation & Agent Rules

To maintain Sprint-grade engineering discipline, all architectural decisions, bugs, and phase transitions are documented in the `development_documentation/` directory. 
- See `INDEX.md` for a complete list of design docs (ADRs) and progress logs.
- See `AGENTS.md` in the root directory for the strict engineering rules and philosophical tenets that all contributors (including AI agents) must follow.
