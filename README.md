# Sciuro 🐿️

Sciuro is an advanced, privacy-first personal finance and asset management application built with Kotlin Multiplatform (KMP). It is designed with rigorous engineering standards to provide full auditability, intelligent automated tracking, and multi-source financial ingestion.

## Key Features

* **Deterministic Self-Transfer Detection:** Two-tier matching engine identifies cross-account transfers using counterparty account numbers extracted from all 7 bank/ewallet notification parsers (Tier 1), falling back to amount+time heuristic only when no account number is present (Tier 2). Masked-number suffix matching handles partially-hidden account numbers (e.g., "...7890", "****7890"). Human-confirmed pairs auto-link on future matches.
* **Account Data Enrichment:** Each account stores its own account number, account holder name, bank code, and QR code image. These fields are set once via the Account Detail edit sheet and enable identity-based transfer matching instead of coincidence-based guessing.
* **QR Code Display:** Bank accounts and e-wallets can store a QR code image (captured from gallery) for quick display when receiving payments. A QR icon button in the account detail hero section opens a large full-screen dialog for scannable display. Cash wallets (Personal Wallet) do not expose QR code features.
* **Obligation / Recurring Bill Tracking:** Pattern-based auto-detection of subscriptions and recurring outflows. Per-transaction cycle matching that automatically advances due dates when a matching payment is booked. Bills can be created, edited, deleted, or deactivated manually.
* **Debt Tracking:** Full CRUD for debts with direction support (I Owe / Owed to Me), progress tracking, counterparty identification, and lifecycle management (Active / Paid Off / Archived). Supports all debt types: loans, credit cards, and informal money owed between people. Automatic payment matching via `DebtEngine` respects direction — repayments owed to you are correctly recognized from incoming transactions.
* **Budget Tracking:** Full CRUD for category budgets with per-category spending limits, progress bars, and reactive spend recalculation. Three-state visual (Healthy / Approaching / Over) with per-budget alert thresholds. Calendar-month boundaries for MONTHLY budgets. Transfer-linked transactions are excluded from spend calculations. Optional rollover carries unused budget to the next period. Create/edit/delete budgets via bottom sheet with category picker and period selector (weekly/monthly/yearly).
* **Malaysian Payment Channels:** Deep integration and detection rules for local payment platforms, physical wallets, and e-wallets.
* **Investment & Gold Savings:** Native support for tracking complex assets like gold and long-term investments.
* **Audit-First Architecture:** Every data mutation passes through a unified Audit Log, ensuring complete traceability.
* **Kanban Workflow:** A unified task management and issue tracking system deeply integrated into the development process.
* **Guided Onboarding:** Initial setup flow securely initializes system ledgers with physical cash on hand.
* **Fast Logging Workflow:** Calculator-first, numpad-driven transaction entry screens with pre-filled category pills and descriptions for near-instant offline transaction recording.
* **UI Standardization & Theming:** Unified design system utilizing custom wrappers (`SciuroBottomSheet`, `SciuroTextField` with inline validation + placeholder + error state, `SciuroCard`, `SciuroPrimaryButton`) across all feature modules. Centralized semantic color tokens (`SignalIncome`, `SignalDanger`, `AccountColor*` presets). Robust 3-way persistent Appearance toggling (Light, Dark, System default). Full accessibility pass: reduced-motion awareness, 44dp touch targets, TalkBack semantics on HeroPanel + confidence indicators + swipe actions. Animated hero figure count-up and chart sparkline with value label.
* **Interactive UI Triage:** Swipe-to-dismiss capabilities for fast transaction approvals and dynamically updated swipeable wallet interfaces to track cash and investments.
* **Proactive Notifications:** Background WorkManager integration periodically alerts users to review newly ingested financial transactions.
* **App Lock:** Optional biometric/PIN gate that secures the app on launch. Re-prompts after 30 seconds of backgrounding. Falls back to a "Set up device security" prompt when no screen lock is enrolled — no silent bypass. Toggled via Settings > Security.

## Project Status

The project is fully functional and has completed **Phase E1 (UI/UX Modernization)**. Core domain modules (Debt, Obligations, Budget, Transfers, Investments) are all wired into the ingestion orchestrator and reactive UI. A Domain Event Bus provides cross-module event-driven communication. The Kanban screen unifies transaction review, bill tracking, and debt overview. A Runway forecast on the Dashboard shows safe-to-spend before next income. The design system is fully standardized — `SciuroTextField` is the single text-input surface across all 8 modules (35 call sites migrated), with built-in inline validation, placeholder, and error state support. Accessibility is hardened: reduced-motion gates on all infinite animations, 44dp min touch targets, and semantic labels on hero panels, confidence indicators, and swipe actions. All core modules—including the multi-source ingestion engine, automated budget tracking with full CRUD, Kanban workflow, and UI feature modules—are fully integrated and tested.

## Architecture

Sciuro is built using a strict modular Kotlin Multiplatform structure:
* **Core Modules** (`core-*`): Reusable domain layers and intelligence engines:
  - `:core-ledger`, `:core-audit`: Foundational persistence and traceability.
  - `:core-ingestion`, `:core-parsing`, `:core-llm`: Notification extraction and LLM fallback parsing.
  - `:core-classifier`: The central Orchestrator that triages parsed data and triggers transfer detection.
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

`HeroPanel` (`core-ui`) is a dark-backed hero header composable used across 8 screens. It renders a title, a large display figure, an optional right-aligned `PillToggle`, an optional `WaveChart`, and a `content` slot for screen-specific extras:

| Screen | heroFigure | chartData | toggle | content slot |
|---|---|---|---|---|
| Dashboard | Total net worth | Real daily balance history | This Month / All Time | Accounts count + weekly adjustments |
| Budgets | Total spent vs allocated | — | — | Top 3 at-risk budgets |
| Account Detail | Account balance | — | — | Adjust Balance + QR icon |
| Debt Overview | I Owe / Owed to Me totals | — | — | Direction breakdown row |
| Kanban | Active tasks / Bills due / Active debts (tab-aware) | — | — | Tab-aware: status breakdown / bill urgency / debt totals |
| Developer Settings | Time since last capture | — | — | Pipeline pending/dead counts |
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

Sciuro includes a full developer settings harness at `feature-settings` > `DeveloperSettingsScreen` with five tabs:

| Tab | Description |
|---|---|
| **Simulator** | Manual pipeline: enter package/title/text and run through all parser rules. Includes a dynamic package+template picker sourced from `FixtureLibrary` (31 fixtures across 7 rules). |
| **Sources** | Read-only view of all 35 allowed notification packages grouped by Bank / E-Wallet / Aggregator. |
| **Ingestion Log** | Dead-letter event viewer with pending/dead-letter counts, per-event error display, and resend capability. |
| **Diagnostics** | Per-rule match/no-match analysis with extracted fields. Shows LLM debug info (prompt, response, latency) when LLM fallback is triggered. |
| **Data Tools** | Clear Inbox (unreviewed transactions) with confirmation dialog. |

**Key classes:**
- `SimulationEngine` (`core-parsing`) — runs the full parser pipeline and captures per-rule results, LLM latency, and debug info in a `SimulationResult`.
- `FixtureLibrary` (`core-parsing`) — shared fixture data (31 cases) used by both tests and the simulator UI.
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
