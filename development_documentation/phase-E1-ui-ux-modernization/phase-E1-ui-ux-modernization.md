# Phase E1: UI/UX Modernization & Component Standardization

## Objective
The primary goal of Phase E1 was to elevate the application's interface from a functional prototype to a cohesive, polished product. This involved standardizing all input components, introducing persistent theme management, and streamlining the manual transaction entry workflow.

## Key Accomplishments

### 1. Component Standardization
- Centralized UI wrappers were introduced in the `:core-ui` module, primarily `SciuroBottomSheet`, `SciuroTextField`, and `SciuroPrimaryButton`.
- Scattered usages of native Material `ModalBottomSheet` and `OutlinedTextField` in feature modules (`feature-dashboard`, `feature-kanban`) were systematically replaced.
- This effectively eliminated design fragmentation and ensured that any future design language changes can be deployed centrally.

### 2. "Calculator-First" Transaction Logging
- The traditional form-based manual entry workflow was deemed inefficient for quick offline transaction logging.
- Developed the `FastTransactionSheet`, a specialized numpad-first interface optimized for rapid numerical input.
- Added quick-select description "pills" (e.g., Breakfast, Groceries, Transport, Others) and category filter chips to minimize the need for on-screen keyboard interaction.
- Expanded the logging flow to natively support inter-account **Transfers** directly from the sheet, enabling dual-entry accounting with a seamless UI.
- The interface automatically scrolls via `verticalScroll(rememberScrollState())` to adapt to smaller screens without clipping the critical "Save Transaction" actions.

### 3. Persistent Appearance Theming
- Established a persistent `ThemeManager` state flow backed by Android `SharedPreferences`.
- Re-architected `SciuroTheme` to reactively observe the user's theme selection (`LIGHT`, `DARK`, `SYSTEM_DEFAULT`).
- Built the `SettingsScreen` UI toggle that allows users to instantly switch themes without requiring an application restart.

### 4. Kanban Layout & Workflow Refinements
- Resolved overlapping and clipping glitches between the Hero Panel, SheetList, and task filtering toggles.
- Redesigned the `KanbanScreen` layout hierarchy to utilize a fixed root `Column`, ensuring the `PillToggle` filter remains sticky at the top of the nested `LazyColumn` for seamless task filtering.
- Enhanced the "Review Transaction" tasks with a manual direction corrector (Income/Expense `SingleChoiceSegmentedButtonRow`), allowing precise reclassification at the point of review before committing to the ledger.

### 5. Final Design System Polish & Hardcoded Debt Cleanup
- Standardized remaining legacy layouts (Settings and Budgets) to use `SciuroCard` and `SciuroTextField`.
- Implemented `AnimatedVisibility` inside `FastTransactionSheet` for fluid, dynamic rendering of context-sensitive fields like 'Category' and 'Destination Account'.
- Fixed `PillToggle` readability issues in Dark Mode by migrating off hardcoded `Color.Black` references to the semantic `MaterialTheme.colorScheme.onSurfaceVariant`.
- Conducted a comprehensive audit of all hardcoded `Color` usages across UI components. Replaced fragmented Hex codes (e.g., Green/Red indicators) with centralized semantic tokens (`SignalIncome`, `SignalDanger`) globally.

### 6. Phase E1.1: Deep Component Completeness & Accessibility Hardening
**Objective:** Close the gap between stated E1 goals and actual codebase state. The phase doc claimed `OutlinedTextField` was replaced project-wide, but grep found 35 remaining call sites across 7 files, plus dormant code, hardcoded colors in theme-critical locations, missing accessibility, and static hero figures.

**6a. SciuroTextField Superset Upgrade (Tier 1)**
- Added `placeholder`, `isError`, `supportingText`, `enabled`, and `minLines` parameters to `SciuroTextField` (`core-ui/components/SciuroComponents.kt:45-59`).
- Migrated all 35 remaining raw `OutlinedTextField` call sites to `SciuroTextField` across `AdjustmentBottomSheet.kt`, `DashboardScreen.kt`, `OnboardingScreen.kt`, `AccountDetailScreen.kt`, `WalletScreen.kt` (12 sites), `DeveloperTabSimulator.kt` (5), `DeveloperTabDiagnostics.kt` (3), `DeveloperTabSources.kt` (1).
- Deleted dormant `app/src/main/java/com/najmi/sciuro/SettingsScreen.kt` (duplicate of `feature-settings/SettingsScreen.kt`, unreferenced).
- Build: all 8 modules pass `compileDebugKotlin`, Detekt clean.

**6b. Theme Token Hygiene (Tier 1)**
- Added 8 `AccountColor*` tokens to `Color.kt` (`AccountColorGreen` through `AccountColorBrown`), replacing raw hex strings in `WalletScreen.kt` account color picker.
- Replaced `Color.LightGray` (not theme-aware) with `MaterialTheme.colorScheme.surfaceVariant` in `LinearProgressIndicator` trackColor across 3 screens (`BudgetsScreen`, `KanbanScreen`, `DebtOverviewScreen`).
- Themed the `SheetList` drag handle from `Color.Gray.copy(0.3f)` to `onSurfaceVariant.copy(0.4f)` (matching `BottomSheetDefaults.DragHandle`).
- Themed bottom-nav unselected colors from `Color.Gray.copy(0.6f)` to `onSurfaceVariant` in `MainActivity.kt`.
- Unified two Dashboard info cards (`Active Budgets`, `Runway`) from raw `Card` to `SciuroCard`.

**6c. Accessibility (Tier 2)**
- Added `reducedMotion()` composable in `Motion.kt` reading `Settings.Global.ANIMATOR_DURATION_SCALE`. Gated all 3 `SciuroMascot` infinite animations (`THINKING` alpha pulse, `CELEBRATE` scale pulse, `REFRESH` rotate) and both Kanban settle `animateFloatAsState` calls — when motion is disabled, animations render at their static end state.
- Increased `PillToggle` option touch targets to 44dp minimum height via `defaultMinSize(minHeight = 44.dp)`.
- Added `contentDescription` to swipe-background icons in `DashboardScreen` ("Approve"/"Reject").
- Added `semantics(mergeDescendants = true) { contentDescription = title }` to `HeroPanel` — TalkBack now reads the full panel as one labelled region.
- Added `semantics { contentDescription = "Confidence N percent" }` to the `TransactionCard` confidence dot (previously invisible to screen readers).

**6d. Motion & Micro-Interactions (Tier 3)**
- Refactored `WaveChart` from raw `Canvas` to `Column(Canvas + Text label)`, showing the current balance below the sparkline.
- Rewrote Kanban settle animation hold time from 2000ms to 1500ms per-item (individual coroutines per trigger already correct).
- Added `animateFloatAsState(count)` count-up animation to `HeroFigure` (gated on `reducedMotion()`). Figures animate from previous value over 500ms `LinearOutSlowInEasing`.

**6e. Forms & Feedback (Tier 4)**
- Added shake animation to `FastTransactionSheet` amount display on invalid input (`Aminatable` translateX, 3 pulses at 45ms each).
- Replaced numpad backspace glyph `?` with `Icons.Filled.Backspace`. Extended `NumpadButton` with optional `icon: ImageVector?` parameter.
- Converted Settings theme picker from `FilterChip` row to `SingleChoiceSegmentedButtonRow` (matching app convention).
- Renamed Settings hero figure from "Config" to "Settings", CategorySettings from "Config" to "Categories".

**6f. Navigation & Layout (Tier 5)**
- Bottom-nav unselected color tokenized (see 6b above).
- Permission gate route conversion deferred (tradeoff: blocking gate UX is correct for a mandatory system permission; routing it opens back-stack edge cases).

## Completed
- [x] Mascot animation motion reduction — static fallbacks for all 3 infinite states via `reducedMotion()`.
- [x] Accessibility audit for animation fallbacks — gated in `SciuroMascot`, `KanbanScreen`, and `HeroFigure`.
- [x] `SciuroTextField` now project-wide single source of truth for text input.

## Next Steps
- Integrate Lottie-based mascot animations for key state moments (Lottie compose dependency present; `.lottie` assets still needed).
- Functional QA pass on foldables/tablets (manual).
- Permission gate route conversion (low-priority polish; current UX is functionally correct).
- Build a `Spacing`/`Elevation` design token table in `core-ui` to replace scattered `dp` literals.
