# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
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
- Fixed runtime crash caused by missing `transferModule` injection in Koin configuration for `DashboardViewModel`.
- SQL database bug where `transaction_record.account_id` was not saved during transaction approval.

### Changed
- Initial project scaffold and documentation structure setup.
