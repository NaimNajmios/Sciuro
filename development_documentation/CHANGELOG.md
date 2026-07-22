# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- Full budget CRUD (create/edit/delete) with bottom sheet UI, category picker, and period selector.
- `updateBudget` and `deleteBudget` queries in `Budget.sq` with corresponding `BudgetRepository` methods and audit logging.
- Category name resolution in `BudgetsViewModel`: budgets now display category names instead of raw IDs.
- Developer Tools Enhancement (DT1-7): Full 5-tab `DeveloperSettingsScreen` restructure with Simulator, Sources, Ingestion Log, Diagnostics, and Data Tools tabs.
- `SimulationEngine` in `core-parsing`: runs the full parser pipeline, captures per-rule match/no-match results, LLM latency, and debug info into a `SimulationResult`.
- `FixtureLibrary` in `core-parsing`: 31 fixtures from all 7 parser rules, shared between tests and simulator UI.
- Dynamic package+template picker in Simulator tab, sourced from `FixtureLibrary` and `IngestionConfig.allowedPackages`.
- Dead-letter event viewer (Tab 3) with pending/dead-letter count strip and per-event error display.
- Parser Diagnostics tab (Tab 4) showing per-rule match/no-match analysis with extracted fields.
- LLM debug panel in Diagnostics tab: prompt, raw response, latency, model used.
- `LlmFallbackParser.lastDebugCapture`: captures prompt/response/latency on every LLM call for developer tooling.
- `ParserTestCase` data class moved from `commonTest` to `commonMain` so it's shared between test fixtures and UI fixtures.
- Confirmation dialog in Data Tools for "Clear Inbox" destructive action.
- Multi-step Onboarding setup flow capturing user's initial cash balance.
- Soft-deletion mechanics for `Account` and `Investment` via new `status` column.
- Undeletable constraint for the core system "Personal Wallet" utilizing a new `is_system` flag.
- Safe `archiveAccount` action in the Wallet UI for non-system accounts.
- Keyboard overlay fixes using `adjustResize` and `Modifier.imePadding()` across `DashboardScreen`, `WalletScreen`, and `OnboardingScreen`.
- Transaction triage mechanics (Approve/Reject) on Dashboard with `SwipeToDismiss` and account selection prompts.
- `HorizontalPager` hero section on the Wallet Screen, enabling dynamic filtering of recent transactions based on the currently swiped account.
- Full Transaction CRUD (Create, Read, Update, Delete) with automatic ledger balance reconciliation and audit logging.
- Background WorkManager reminders that push periodic local notifications every 30 minutes for unreviewed transactions.
- Persistent categorized transaction logging (Income/Expense pills) supported by automated schema seeding.
- Direct "Reject" action button alongside "Approve" for Kanban task review.
- UI Standardization: Replaced all scattered inline modals, text fields, and buttons with a centralized `core-ui` design system using `SciuroBottomSheet`, `SciuroTextField`, and `SciuroPrimaryButton`.
- "Calculator-First" Fast Transaction logging using `FastTransactionSheet` for immediate manual numerical input.
- System-wide 3-way Appearance theme toggling (Light, Dark, System) managed by persistent `ThemeManager` in the new Settings view.
- Kanban UI refactoring, migrating the status filter layout into a sticky `PillToggle` above the task list for rapid filtering.
- Dedicated `DeveloperSettingsScreen` to isolate development and simulation tools from the primary user Settings menu.
- Removed arbitrary pagination/limiting in the Wallet Account Associated App selection, ensuring all installed packages are searchable.
- Added manual transaction direction correction (Income/Expense segmented toggle) to the Kanban "Review Transaction" task flow, enabling precise classification before ledger commit.
- Expanded `FastTransactionSheet` preset label options to include an "Others" pill.
### Fixed
- Budget empty state CTA now opens creation sheet (was a no-op).
- Budget cards now show category names via in-memory join with `CategoryRepository`.
- Fixed runtime crash caused by missing `transferModule` injection in Koin configuration for `DashboardViewModel`.
- SQL database bug where `transaction_record.account_id` was not saved during transaction approval.

### Changed
- Initial project scaffold and documentation structure setup.
