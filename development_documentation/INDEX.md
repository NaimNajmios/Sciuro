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
| B2 - Transfer detection | Completed | Original `TransferDetectionEngine` with amount+time heuristic (superseded by Acct+Transfer phases) |
| B3 - Balance & reconciliation engine | Completed | `ReconciliationEngine` and `CashAdjustment` schemas to fix ledger drift |
| B4 - Manual Review Inbox | Completed | Exposed `observeUnreviewedTransactions` Flow for UI consumption |
| B5 - Debt Ledger module | Completed | `core-debt` scaffolded, `DebtEngine` implemented for automatic payment tracking |
| B6 - Investment/Gold Savings module | Completed | `core-investment` scaffolded, tracks asset accumulation independently |
| B7 - Budgeting logic | Completed | `core-budget` scaffolded, `BudgetEngine` implemented for rolling 30-day tracking |
| B7.5 - Budget Feature Completion | Completed | Full budget CRUD (create/edit/delete), category name resolution via `CategoryRepository`, creation bottom sheet with category picker/period selector, edit/delete flows with destructive confirmation |
| C1 - Kanban board | Completed | `feature-kanban` UI scaffolded with Jetpack Compose and ViewModels |
| C2 - Home dashboard & Wallet screen | Completed | Scaffolded `feature-dashboard` and `feature-wallet` UI with ViewModels |
| C3 - Drilldown screens | Completed | Scaffolded `feature-budgets` UI and assembled app Navigation |
| D1 - Security hardening | Completed | Disabled Android auto-backup for the database domains |
| D2 - Full test pass & dogfood | Completed | Connected UI ViewModels to real SQLDelight reactive flows |
| D3 - Personal deployment | Completed | Sideloaded APK built with multiple architecture support |
| D4 - Initial Setup & Onboarding | Completed | Compose navigation for initial Personal Wallet setup and account soft-deletion schemas |
| E1 - UI/UX Modernization | Completed | Standardized UI wrappers, 3-way Appearance theming, FastTransactionSheet numpad workflow, Kanban sticky filter |
| A3.5 - Reliability Hardening | Completed | Fault isolation (per-event try/catch, restart), durable capture (RawEventStaging table), direction bug fix (nullable, inflow keywords, confidence scoring), LLM hardening (HttpTimeout, retry, circuit breaker, externalized config), OEM resilience (lifecycle hooks, WorkManager reconciliation, explicit manifest) |
| DT1-7 - Developer Tools Enhancement | Completed | 5-tab DeveloperSettingsScreen (Simulator, Sources, Ingestion Log, Diagnostics, Data Tools), SimulationEngine with per-rule diagnostics, FixtureLibrary (31 fixtures shared between tests and UI), dynamic package+template picker, dead-letter viewer, LLM debug panel, ParserTestCase moved to commonMain |
| **Acct-1–3 — Account Data Enrichment** | **Completed** | Extended account schema with number, holder name, bank code, QR image. Added edit bottom sheet and QR capture/display on detail screen. Created `account_pair_confirmation` table for tracking human-confirmed transfer pairs. |
| **Transfer-1–2 — Deterministic Self-Transfer Detection** | **Completed** | Two-tier detection: identity-based matching via counterparty account number (Tier 1) with heuristic fallback (Tier 2). Per-transaction reactive engine replaces batch scan. Masked-suffix matching for partial/masked account numbers. |
