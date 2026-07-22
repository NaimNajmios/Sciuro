# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- **Net Position Reporting**: Updated DashboardViewModel and DashboardScreen to calculate and display a unified Net Position: `(Total Account Balances + Total Investment Values) - Total Debts`.
- **Motion Tokens (UX-1)**: Implemented `SciuroMotion` standard animation specs (`micro`, `transitionSpec`, `cardMove`, `celebration`, `count`) in `core-ui/theme` as the single source of truth for motion.
- **Screen Transitions (UX-2)**: Added custom animated screen transitions (`fadeIn`, `fadeOut`, `slideIntoContainer`, `slideOutOfContainer`) via Compose Navigation across the entire `MainActivity` NavHost.
- Full-screen swiping architecture: Dashboard, Kanban, Wallet, and Budgets screens now share a consistent root `Box` + `LazyColumn` layout. HeroPanel scrolls off-screen; SheetList fills the viewport with `fillParentMaxHeight()`. Inner content uses `Column` + `forEach` (no nested `LazyColumn`). FAB overlaid in the root `Box` with `Modifier.align(Alignment.BottomEnd)`.

### Changed
- `HeroPanel` hero figure typography reduced from `displayLarge` to `headlineLarge` to prevent number overflow on large figures.
- WalletScreen custom hero figure typography reduced from `displayLarge` to `headlineLarge`.
- KanbanScreen: root `Column` → `Box` + `LazyColumn`; `SheetList` modifier `.weight(1f)` → `.fillParentMaxHeight()`; inner `LazyColumn` → `Column` + `filteredTasks.forEach`.
- WalletScreen: root `Column` → `Box` + `LazyColumn`; `SheetList` modifier `.weight(1f)` → `.fillParentMaxHeight()`; inner `LazyColumn` → `Column` + forEach for transactions/adjustments; FAB moved into root `Box` overlay.

### Fixed
- Number overflow in hero figures for Dashboard (Total Net Worth), Kanban (Active Debt), Budgets (spent/allocated), and Wallet (Total Liquidity/Investments) by switching to `headlineLarge` typography.
- Wired `BudgetEngine` and `DebtEngine` directly into the `SciuroIngestionOrchestrator` transaction booking path to ensure downstream cascade architectures fire immediately.
- Connected `OnboardingScreen` into `MainActivity`'s `NavHost` with a dynamic `startDestination` and a `LaunchedEffect` observer to properly transition users upon completing the first-launch setup.
- Removed dead/duplicate `TransactionInspectorSheet.kt` code to enforce `TransactionDetailSheet.kt` as the single source of truth for transaction inspection.
- Fixed a malformed `{` brace parsing error inside `WalletScreen.kt` `SheetList`.

### Added
- **Database Encryption (SQLCipher)**: Integrated `net.sqlcipher.database` into `core-ledger`. The SQLDelight driver now initializes with a 256-bit passphrase managed securely via `EncryptedSharedPreferences` (Android KeyStore).
- **Biometric App Gate**: Wrapped `SciuroMainScreen` with a `BiometricGate` requiring fingerprint/face/PIN authentication before the application UI is accessible.
- `content` slot on `HeroPanel` — `@Composable ColumnScope.() -> Unit = {}`, renders after chart inside the dark hero surface. Fully backward-compatible (defaults to `{}`).
- Dashboard: replaced mock chart data with real `balanceHistory` computed from daily-aggregated running balance of all transactions. Toggle slices the series (last 30 days for "This Month", full series for "All Time"). Secondary `content` row shows accounts count + weekly adjustment count.
- Budgets: `heroFigure` now shows `"RM X / RM Y"` (total spent vs allocated) instead of a bare count. `content` slot lists top 3 at-risk budgets by spend progress.
- Account Detail: absorbed the manually-wired "Adjust Balance" button into the `content` slot, removing the outer `Column(background(SurfaceHero))` wrapper.
- Developer Settings: `heroFigure` now shows time since last notification capture (e.g. "12m ago") via `rawEventRepository.getLastCapturedAt()`. `content` slot shows pending and dead-letter counts.
- Kanban: `content` slot shows Upcoming/Due/Settled task counts from actual task status distribution.
- `SettingsViewModel._lastCapturedAt` StateFlow added, populated in `refreshCounts()`.
- Extended `account` schema with `account_number`, `account_holder_name`, `bank_institution_code`, `qr_image_path`, `qr_payload_text` — schema migration `3.sqm` with matching `Account.sq` CREATE TABLE updates.
- Created `account_pair_confirmation` table for tracking human-confirmed transfer pairs (auto-inserted on manual link).
- Added `counterpartyAccountNumber` to `StructuredDraft` with regex extraction in `RegexExtractors.kt` targeting Malaysian bank notification patterns (A/C, Account, Acc).
- Added `matchesAccountSuffix()` helper for masked-number suffix matching (strips non-digits, compares last N digits).
- Updated `CimbParserRule` to extract counterparty account number from notification text and boost confidence score.
- Restructured `TransferDetectionEngine`: replaced batch `runDetection()` with per-transaction `onTransactionBooked()`. Two-tier architecture — Tier 1 matches by identity (counterparty account number), Tier 2 falls back to amount+time heuristic for notifications without account numbers. First-time heuristic pairs auto-link only after human confirmation.
- Wired `TransferDetectionEngine` in `SciuroIngestionOrchestrator` after transaction booking, passing the draft's counterparty account number.
- `TransferRepository.linkTransactions()` now auto-records account pair confirmation for future heuristic auto-linking.
- Added "Edit Details" bottom sheet to AccountDetailScreen with fields for account number, account holder name, bank code, and QR code management.
- Added QR code image capture via system photo picker (`ActivityResultContracts.GetContent`), file copy to `filesDir/qr_codes/`, thumbnail display on account detail screen, and fullscreen dialog on tap.
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
### Added
- Counterparty account number extraction to all 6 remaining parser rules: Maybank, BSN, TnG, GrabPay, Boost, ShopeePay. Now all 7 parsers populate `counterpartyAccountNumber` on their `StructuredDraft` with matching confidence boost (+0.1f).
- `endingAccountNumberRegex` in `RegexExtractors.kt` for English "ending XXXX" and Malay "berakhir XXXX" patterns, in addition to the existing `accountNumberRegex` for "A/C XXXX" style.
- `expectedCounterpartyAccountNumber` field to `ParserTestCase` and assertion in `runParserTests()` — test infrastructure now validates account number extraction.
- `RegexExtractorsTest.kt` (16 tests): dedicated test suite covering `extractAmount`, `extractMerchant`, `extractAccountNumber` (all regex variants, masked, null cases), and `matchesAccountSuffix` (suffix match, mask chars, mismatch, empty input).
- JVM target to `core-audit`, `core-ledger`, and `core-transfer` modules with JDBC SQLite driver for in-memory database testing.
- `TransferDetectionEngineTest.kt` (10 integration tests): Tier 1 deterministic matching (suffix, masked, cross-account, time-agnostic, amount mismatch), Tier 2 heuristic (confirmed pair, unconfirmed pair, outside 2-min window).
### Changed
- `accountNumberRegex` in `RegexExtractors.kt` now falls through to `endingAccountNumberRegex` for English/YMalay ending patterns — all existing CIMB/Maybank fixture texts with "ending XXXX" or "berakhir XXXX" now extract account numbers.
### Fixed
- Budget empty state CTA now opens creation sheet (was a no-op).
- Budget cards now show category names via in-memory join with `CategoryRepository`.
- Fixed runtime crash caused by missing `transferModule` injection in Koin configuration for `DashboardViewModel`.
- SQL database bug where `transaction_record.account_id` was not saved during transaction approval.

### Changed
- Initial project scaffold and documentation structure setup.
