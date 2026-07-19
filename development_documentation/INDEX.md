# Sciuro Development Documentation

## Phase Status
| Phase | Status | Summary |
|---|---|---|
| A0 - Engineering foundations | Completed | Module structure, Koin DI, detekt/ktlint, CI scaffold, SQLDelight + migrations, `development_documentation/` scaffold, test-tier strategy |
| A1 - Audit Log core | Completed | AuditLog entity, SQLDelight schema, AuditRepository, and Repository-wrapper pattern |
| A2 - Ingestion framework | Completed | `IngestionSource` abstraction, `NotificationSourceAdapter`, and staging buffer |
| A3 - Bank & e-wallet parsers | Completed | CIMB, Maybank, BSN, TNG, GrabPay, Boost, ShopeePay and fixture regression suite |
| A4 - LLM-assisted fallback | Completed | Groq Llama 3 API integration, Opt-in architecture, `SciuroParserPipeline` |
| A5 - Financial taxonomy & data model | Completed | Ledger SQLDelight schemas (Account, Category, TransactionRecord) and Koin Repositories |
| A6 - Actor-critic triage & categorization | Completed | `SciuroIngestionOrchestrator`, basic static heuristic engine, inbox routing |
| B1 - Recurring obligation & debt auto-detection | Completed | `ObligationDetectionEngine` to scan ledger for recurring merchant patterns |
| B2 - Transfer detection | Completed | `TransferDetectionEngine` links dual INFLOW/OUTFLOW notifications |
| B3 - Balance & reconciliation engine | Completed | `ReconciliationEngine` and `CashAdjustment` schemas to fix ledger drift |
| B4 - Manual Review Inbox | Completed | Exposed `observeUnreviewedTransactions` Flow for UI consumption |
| B5 - Debt Ledger module | Completed | `core-debt` scaffolded, `DebtEngine` implemented for automatic payment tracking |
| B6 - Investment/Gold Savings module | Completed | `core-investment` scaffolded, tracks asset accumulation independently |
| B7 - Budgeting logic | Completed | `core-budget` scaffolded, `BudgetEngine` implemented for rolling 30-day tracking |
| C1 - Kanban board | Completed | `feature-kanban` UI scaffolded with Jetpack Compose and ViewModels |
| C2 - Home dashboard & Wallet screen | Completed | Scaffolded `feature-dashboard` and `feature-wallet` UI with ViewModels |
| C3 - Drilldown screens | Completed | Scaffolded `feature-budgets` UI and assembled app Navigation |
| D1 - Security hardening | Not Started | |
| D2 - Full test pass & dogfood | Not Started | |
| D3 - Personal deployment | Not Started | |
