# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- **Phase F4 — Auto-Confirm, Event Publishers, OEM Guidance**: Added `autoConfirmEnabled`/`autoConfirmThreshold` settings. Created `ConfidenceTracker` using P3's `merchant_category_rule` table. `ObligationDetectionEngine` now publishes `RecurringObligationConfirmed` for trusted merchants (3+ confirmations) or `RecurringObligationProposed` for first-time detections. `TransferDetectionEngine` wired to publish `TransferMatched` (on link) and `TransferUnmatchedFlagged` (on unconfirmed heuristic). `IncomeRecurrencePatternDetector` wired to publish `IncomeRecurrencePatternDetected` via `detectAndPublish()`. Created `OemAutostartHelper` for Xiaomi/OPPO/Vivo/Huawei autostart guidance. Added "Auto-confirm recurring bills" toggle to Settings.
### Changed
- **Wallet Cards — Type-Aware Icons & Labels**: Wallet cards now differentiate between Cash/Personal wallets, Bank Accounts, and E-Wallets with correct icons (`Wallet` for cash, `AccountBalance` for bank, `AccountBalanceWallet` for e-wallet) and correct subtitles ("Cash Wallet", "Bank Account", "E-Wallet"). Previously all non-e-wallet accounts unconditionally displayed "Bank Account" with the bank icon regardless of the actual `type` column value.
- **QR Code UX Overhaul**: Removed the dedicated white QR thumbnail section that sat between the hero header and the transaction bottom sheet on AccountDetailScreen. The QR code is now accessible via a compact `FilledTonalButton` icon in the dark hero section alongside the "Adjust Balance" button, triggering a full-screen dialog with a large, scannable QR image. This reclaims screen real estate and preserves visual continuity between the dark hero and the bottom sheet.
### Fixed
- **Personal Wallet Hides Bank Features**: Personal Wallet (type `"Cash"`) no longer exposes bank-specific features. The QR code display button is hidden on the account detail screen. The "Edit Details" bottom sheet hides the entire QR code picker/remover section for cash wallets. Previously these features were shown for all account types.
- **Phase B5.5 — Debt Module Completion**: Added `direction` (I_OWE / OWED_TO_ME), `counterparty_name`, `status` (ACTIVE / PAID_OFF / ARCHIVED), and `notes` fields to `debt_record` via migration `4.sqm`. Created `feature-debt` module with full CRUD UI: debt list split by direction, progress bars, create/edit bottom sheet, payment recording for informal debts, and delete with confirmation. Fixed `DebtEngine` direction matching — OWED_TO_ME debts now match INFLOW transactions instead of being silently ignored. Fixed `DashboardViewModel` Net Position sign: OWED_TO_ME debts are now treated as receivables (added) rather than liabilities (subtracted).
- **Phase B1.5 — Obligation Module Activation**: Wired `obligationsModule` into Koin DI (previously missing from `SciuroApp.kt`). Added `observeActiveObligations`, `observeAllObligations`, `updateObligation`, `deleteObligation`, `deactivateObligation`, and `advanceNextDueDate` to `ObligationRepository`. Created `ObligationCycleMatcher` for per-transaction settlement matching (merchant or category+amount heuristics). Wired `ObligationCycleMatcher` into `SciuroIngestionOrchestrator` after transfer detection, so recurring bill cycles advance automatically when matching payments are booked.
- **Phase C1.5 — Kanban Unification**: Refactored KanbanScreen with three tabs (Review / Bills / Debts) via PillToggle. Review tab uses the extracted KanbanTaskCard composable (no longer dead code). Bills tab shows obligations with computed status (Upcoming / Due Soon / Overdue) and "Mark as Paid" action that books a manual transaction. Debts tab shows active debts with progress bars and "Record Payment" for informal debts. HeroPanel content is tab-aware — shows bill urgency or debt totals per tab. Added BillTask and DebtTask model classes with status derivation from obligation due dates. Added `:core-obligations`, `:core-debt` dependencies to feature-kanban.
- **Domain Event Bus (infrastructure)**: Implemented `DomainEventBus` (SharedFlow-based Koin singleton) and `DomainEvent` sealed interface in `core-audit/events/`. Provides event types: DebtBalanceUpdated, DebtFullyPaidOff, ObligationCycleSettled, ObligationCreated, BudgetThresholdCrossed. Registered as singleton in ledgerModule.
- **Phase B7.6 — Budget Enhancement Round 2**: Calendar-month alignment — BudgetEngine now uses proper month boundaries (via Calendar) for MONTHLY budgets instead of a rolling 30d window. Transfer-exclusion fix — BudgetEngine excludes any transaction linked in the transfer_link table from spend calculations. Per-budget threshold support — added `alertThresholdPercent` to Budget model, BudgetsScreen reads per-budget threshold (falls back to global Settings default). Three-state visual — BudgetHealth enum (HEALTHY / APPROACHING / OVER) drives progress bar color. Rollover — BudgetEngine computes effective allocation with carry-over from previous period when `rollover = true`. BudgetThresholdCrossed event published via DomainEventBus when a budget passes its threshold.
- **Phase C4 — Runway / Cash Flow Forecast**: Created `IncomeRecurrencePatternDetector` in core-obligations (detects recurring INFLOW patterns by merchant with amount similarity and interval analysis). Dashboard now shows a Runway metric — liquid balances plus expected next income minus obligations and debts due before next income. Displayed as a secondary card alongside Active Budgets. Shows "based on bills only" caveat when no income pattern is detected yet.
- **Phase X1 — Engine Activation & Import Cleanup**: Activated `InvestmentEngine.processInvestments()` (auto-tracks investment purchases from transactions) and `ObligationDetectionEngine.runDetection()` (auto-creates recurring obligations from ≥3 similar merchant outflows) in the ingestion orchestrator fan-out. Both engines were registered in Koin but never called. Cleaned up inline fully-qualified type references for `BudgetEngine`, `DebtEngine`, and `RawEvent` in `SciuroIngestionOrchestrator` (replaced with proper imports). Added `:core-investment` dependency to `core-classifier`.
- **Phase X2 — Transfer Exclusion for Debt & Investment Engines**: Applied the same `transferTxIds` exclusion pattern that `BudgetEngine` uses to `DebtEngine.processDebtPayments()` and `InvestmentEngine.processInvestments()`. Prevents self-transfers that coincidentally match a debt name or investment asset from incorrectly adjusting balances.
- **Phase X3 — SettingsProvider & LlmParsingConfig Relocation**: Moved `SettingsProvider` interface and `LlmParsingConfig` data class from `core-parsing/config/` to `core-ledger/config/` (package `com.sciuro.core.ledger.config`). This removes the misplaced dependency where four feature modules depended on all of `core-parsing` solely for settings access. Dropped now-unused `:core-parsing` gradle edges from `feature-dashboard`, `feature-wallet`, `feature-budgets`, and `feature-kanban` (the latter was a dead edge with zero imports). Added explicit `:core-ledger` dependency to `core-parsing` to replace the transitive edge.
- **Phase X4 — Domain Event Bus Completion**: Wired `DomainEventBus` into all three engines that had defined-but-never-published events. `DebtEngine` now publishes `DebtBalanceUpdated` and `DebtFullyPaidOff` when a debt's remaining balance changes. `ObligationCycleMatcher` now publishes `ObligationCycleSettled` when a transaction matches and advances a recurring obligation's due date. `ObligationDetectionEngine` now publishes `ObligationCreated` when a new recurring pattern is detected. In `feature-kanban`, `KanbanViewModel` subscribes to event bus events and exposes `animationTriggers`; `KanbanScreen` collects them and applies a brief scale animation (`animateFloatAsState` + `graphicsLayer`) on bill and debt cards that were just settled or updated, satisfying UAT checklist items 465/525.

### Added
- **Dynamic System Configurations**: Upgraded hardcoded application constants into user-configurable preferences backed by `SettingsProvider`.
  - Added an interactive **Budget Warning Threshold** slider in Settings to let users decide when budget progress bars turn red (scales 50%-100%).
  - Built a dynamic **Quick Labels** engine allowing custom presets in `FastTransactionSheet`.
  - Enabled **LLM Engine Hot-swapping** by exposing the Groq Model String in the Developer Settings UI.
  - Initialized **Category Management UI**, exposing the `deleteCategory` and `updateCategory` data layer directly to the user.
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
- **App Lock Toggle & Resume Re-prompt**: Added `isLockEnabled` / `setLockEnabled` to `SettingsProvider` interface (persisted in `EncryptedSharedPreferences`, default off). `BiometricGate` now accepts a `lockEnabled` parameter — when disabled, renders content immediately. When enabled, observes `ProcessLifecycleOwner` lifecycle with `DisposableEffect`: records `backgroundedAt` on `ON_STOP`, resets `isAuthenticated` on `ON_START` if ≥30s elapsed, and increments an `authAttempt` counter to re-trigger the `LaunchedEffect`-driven `BiometricPrompt`. Added `androidx.lifecycle:lifecycle-process` dependency. Added "Security" card with "Lock app on launch" `Switch` to the main Settings screen in `feature-settings`.
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
