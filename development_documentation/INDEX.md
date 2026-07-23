# Sciuro Development Documentation

## Phase Status
| Phase | Status | Summary |
|---|---|---|---|
| A0 - Engineering foundations | Completed | Module structure, Koin DI, detekt/ktlint, CI scaffold, SQLDelight + migrations, `development_documentation/` scaffold, test-tier strategy |
| A1 - Audit Log core | Completed | AuditLog entity, SQLDelight schema, AuditRepository, and Repository-wrapper pattern |
| A2 - Ingestion framework | Completed | `IngestionSource` abstraction, `NotificationSourceAdapter`, and staging buffer |
| A3 - Bank & e-wallet parsers | Completed | CIMB, Maybank, BSN, TNG, GrabPay, Boost, ShopeePay and fixture regression suite |
| A4 - LLM-assisted fallback | Completed | Groq Llama 3 API integration, Opt-in architecture, `SciuroParserPipeline` |
| A5 - Financial taxonomy & data model | Completed | Ledger SQLDelight schemas (Account, Category, TransactionRecord) and Koin Repositories |
| A6 - Actor-critic triage & categorization | Completed | `SciuroIngestionOrchestrator`, basic static heuristic engine, inbox routing |
| B1 - Recurring obligation & debt auto-detection | Completed | `ObligationDetectionEngine` to scan ledger for recurring merchant patterns |
| B1.5 - Obligation Module Activation | Completed | Wired `obligationsModule` in Koin DI. Added repository CRUD and reactive observe. Created `ObligationCycleMatcher` for per-transaction cycle settlement. Wired into ingestion orchestrator. |
| B2 - Transfer detection | Completed | Original `TransferDetectionEngine` with amount+time heuristic (superseded by Acct+Transfer phases) |
| B3 - Balance & reconciliation engine | Completed | `ReconciliationEngine` and `CashAdjustment` schemas to fix ledger drift |
| B4 - Manual Review Inbox | Completed | Exposed `observeUnreviewedTransactions` Flow for UI consumption |
| B5 - Debt Ledger module | Completed | `core-debt` scaffolded, `DebtEngine` implemented for automatic payment tracking |
| B5.5 - Debt Module Completion | Completed | Added direction/status/counterparty to debt model. Created `feature-debt` module with full CRUD UI (view, add, edit, delete, record payment). Fixed DebtEngine direction matching for OWED_TO_ME debts. Fixed DashboardViewModel Net Position sign for receivable debts. |
| B6 - Investment/Gold Savings module | Completed | `core-investment` scaffolded, tracks asset accumulation independently |
| B7 - Budgeting logic | Completed | `core-budget` scaffolded, `BudgetEngine` implemented for rolling 30-day tracking |
| B7.5 - Budget Feature Completion | Completed | Full budget CRUD (create/edit/delete), category name resolution via `CategoryRepository`, creation bottom sheet with category picker/period selector, edit/delete flows with destructive confirmation |
| B7.6 - Budget Enhancement Round 2 | Completed | Calendar-month alignment (MONTHLY budgets), transfer-link exclusion from spend, per-budget alert threshold with three-state visual (HEALTHY/APPROACHING/OVER), rollover carry-over, BudgetThresholdCrossed event on DomainEventBus |
| C1 - Kanban board | Completed | `feature-kanban` UI scaffolded with Jetpack Compose and ViewModels |
| C1.5 - Kanban Unification | Completed | Added Review/Bills/Debts tabs to KanbanScreen. Bills column with status derivation (Overdue/Due Soon/Upcoming). Debts column with progress bars and payment recording. KanbanTaskCard refactored from dead code into active use. Hero panel tab-aware. |
| C4 - Runway / Cash Flow Forecast | Completed | IncomeRecurrencePatternDetector, Runway metric on Dashboard (liquid balances + income - upcoming obligations/debts), "based on bills only" caveat |
| X1 - Engine Activation | Completed | Activated `InvestmentEngine` and `ObligationDetectionEngine` in orchestrator fan-out. Cleaned up inline FQ types in orchestrator. Added `:core-investment` dep to classifier. |
| X2 - Transfer Exclusion (Debt/Investment) | Completed | Applied `transferTxIds` exclusion to `DebtEngine` and `InvestmentEngine` (matching `BudgetEngine`'s existing pattern). Prevents self-transfer false matches. |
| X3 - SettingsProvider Relocation | Completed | Moved `SettingsProvider` + `LlmParsingConfig` from `core-parsing` to `core-ledger`. Dropped extraneous `:core-parsing` edges from dashboard, wallet, budgets, and kanban. |
| X4 - Domain Event Bus Completion | Completed | Wired event publishers in `DebtEngine`, `ObligationCycleMatcher`, `ObligationDetectionEngine`. Added `KanbanViewModel` event subscription with animation trigger flow. KanbanScreen shows settle animation on bill/debt cards (UAT 465/525). |
| C2 - Home dashboard & Wallet screen | Completed | Scaffolded `feature-dashboard` and `feature-wallet` UI with ViewModels |
| C3 - Drilldown screens | Completed | Scaffolded `feature-budgets` UI and assembled app Navigation |
| D1 - Security hardening | Completed | Disabled Android auto-backup for the database domains. Added optional biometric/PIN `BiometricGate` with user-facing toggle (Settings > Security). Gate observes `ProcessLifecycleOwner` to re-prompt after 30s of backgrounding. Falls back to a "Set up device security" prompt when no screen lock is enrolled. |
| D2 - Full test pass & dogfood | Completed | Connected UI ViewModels to real SQLDelight reactive flows |
| D3 - Personal deployment | Completed | Sideloaded APK built with multiple architecture support |
| D4 - Initial Setup & Onboarding | Completed | Compose navigation for initial Personal Wallet setup and account soft-deletion schemas |
| E1 - UI/UX Modernization | Completed | Standardized UI wrappers, 3-way Appearance theming, FastTransactionSheet numpad workflow, Kanban sticky filter |
| A3.5 - Reliability Hardening | Completed | Fault isolation (per-event try/catch, restart), durable capture (RawEventStaging table), direction bug fix (nullable, inflow keywords, confidence scoring), LLM hardening (HttpTimeout, retry, circuit breaker, externalized config), OEM resilience (lifecycle hooks, WorkManager reconciliation, explicit manifest) |
| DT1-7 - Developer Tools Enhancement | Completed | 5-tab DeveloperSettingsScreen (Simulator, Sources, Ingestion Log, Diagnostics, Data Tools), SimulationEngine with per-rule diagnostics, FixtureLibrary (31 fixtures shared between tests and UI), dynamic package+template picker, dead-letter viewer, LLM debug panel, ParserTestCase moved to commonMain |
| **Acct-1–3 — Account Data Enrichment** | **Completed** | Extended account schema with number, holder name, bank code, QR image. Added edit bottom sheet and QR capture/display on detail screen. Created `account_pair_confirmation` table for tracking human-confirmed transfer pairs. |
| **Transfer-1–2 — Deterministic Self-Transfer Detection** | **Completed** | Two-tier detection: identity-based matching via counterparty account number (Tier 1) with heuristic fallback (Tier 2). Per-transaction reactive engine replaces batch scan. Masked-suffix matching for partial/masked account numbers. |
| **Hero-1–6 — Hero Panel Enhancement** | **Completed** | Added `content` slot to `HeroPanel`. Dashboard uses real balance history chart instead of mock data. Budgets hero shows allocated-vs-spent totals with at-risk mini-rows. AccountDetail absorbs Adjust button into slot. DeveloperSettings shows pipeline health (last capture time + ingestion counts). Kanban shows column breakdown counts. |
| F1 — Configurable allowlist & parser health | Completed | Runtime-editable allowlist via `MutableIngestionAllowlist` + `SettingsProvider`. Enabled "Add Custom Package" in `DeveloperTabSources`. Added `ParserHealthRepository` + `DeveloperTabHealth` tab with per-package match-rate monitoring. |
| F2 — DomainEvent catalog completion | Completed | Extended `DomainEvent` sealed interface from 5 to 23 event types matching automation cascade plan §3. No publishers or consumers changed — unblocks P3/P4. |
| F3 — Rule Learner, CategoryResolver, BudgetLimitSuggester | Completed | Migration `5.sqm` with `merchant_category_rule` + `recipient_rule` tables. `RuleLearner` subscribes to event bus and learns merchant→category. `CategoryResolver` replaces inline `guessCategoryId()`. `BudgetLimitSuggester` with 90-day trimmed mean. Budget creation sheet shows suggestion chip. |
| F4 — Auto-confirm, event publishers, OEM guidance | Completed | `ConfidenceTracker` for merchant trust, `ObligationDetectionEngine` auto-confirm, `TransferDetectionEngine` publishes `TransferMatched`/`TransferUnmatchedFlagged`, `IncomeRecurrencePatternDetector` publishes via `detectAndPublish()`, `OemAutostartHelper`, auto-confirm toggle in Settings. |
| F5 — Investment PriceProvider + Valuation Engine | Completed | `PriceProvider` interface + `ManualPriceProvider` (user-set). Migration `6.sqm` for `unit_type`. `InvestmentValuationEngine` with `getTotalCurrentValue()`. `InvestmentEngine` publishes `InvestmentTransactionRecorded`. |
| F6 — Debt payment link table | Completed | Migration `7.sqm` + `DebtPaymentLinkRepository`. `DebtEngine` rewritten to use link-based matching with `UNIQUE(debt_id, transaction_id)` for structural idempotency. Prevents double-counting. |
| **HS-1–3 — Hero Section Refactoring & Full-Screen Swiping** | **Completed** | Fixed number overflow by switching hero figure typography to `headlineLarge`. Refactored Kanban, Wallet, and Dashboard screens to use a single root `LazyColumn` pattern for full-screen swiping (hero scrolls off, sheet fills viewport). Standardized `SheetList` modifier to `fillParentMaxHeight()` across all screens. Replaced nested `LazyColumn` instances with `Column` + `forEach` inside `SheetList`. FAB moved into `Box` overlay for consistent z-ordering. |
