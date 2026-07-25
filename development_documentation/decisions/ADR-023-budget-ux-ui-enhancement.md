# ADR 023: Budget Module UX/UI Enhancement

## Context
A comprehensive UX/UI audit of the budget module identified critical failures, significant friction points, and visual inconsistencies. The audit followed the UX Design Enhancement Prompt and UI Design Enhancement Prompt frameworks across 10 dimensions each (goal clarity, flow, cognitive load, feedback, error prevention, discoverability, efficiency, trust, emotion, edge cases for UX; layout, typography, color, iconography, components, motion, elevation, states, dark mode, polish for UI).

Key problems identified:
- **Critical:** HeroPanel title hardcoded to "Monthly Budgets" even with weekly/yearly budgets
- **Critical:** `CategoryDrilldownScreen` orphaned — no navigation entry point from anywhere
- **Critical:** Zero success feedback after create/edit/delete mutations
- **Critical:** `CategoryDrilldownScreen` dead end — no back button
- **High:** Non-destructive actions (create, save) guarded by unnecessary confirmation dialogs
- **High:** Fake pull-to-refresh (600ms delay, no actual data fetch)
- **High:** Nested `LazyColumn` inside `SheetList` violating AGENTS.md convention
- **Medium:** FAB invisible in dark mode (primary = #FFFFFF, onPrimary = #000000 on dark)
- **Medium:** Category icon background using raw `Color.parseColor()` bypassing theme tokens
- **Medium:** No inline validation for amount input
- **Medium:** Inconsistent progress bar heights (8dp vs 6dp)

## Decision

### 1. Remove Non-Destructive Confirmation Dialogs
Removed `showCreateConfirmation` and `showSaveConfirmation` state variables and their `SciuroConfirmationDialog` composables. Create and save now execute directly on button tap. Delete confirmation retained (destructive action).

**Rationale:** Nielsen's Heuristic #5 (Error Prevention) applies to destructive actions only. Confirming non-destructive actions adds friction without safety benefit. The wallet module follows the same pattern — no confirmation on create/save.

### 2. Snackbar Feedback via Composition Local
Added `LocalSnackbarHostState.current` in `BudgetsScreen` and `CategoryDrilldownScreen`. All mutation callbacks wrapped in `coroutineScope.launch { ...; snackbarHostState.showSnackbar(...) }`. Messages: "Budget created for [Category]", "Budget updated", "Budget deleted".

**Rationale:** Matches the established pattern used by `WalletScreen`, `AccountDetailScreen`, `DashboardScreen`, and `KanbanScreen` (42 existing `showSnackbar` call sites).

### 3. Suspend ViewModel Mutations
Changed `createBudget`, `updateBudget`, `deleteBudget` from fire-and-forget (`viewModelScope.launch`) to `suspend fun` with `withContext(Dispatchers.IO)`. Callers now await completion before showing snackbar.

**Rationale:** Fire-and-forget caused snackbar to show before DB write completed, creating a race condition where the list might not yet reflect the change.

### 4. Dynamic HeroPanel Title
Changed from `"Monthly Budgets"` to `"Budgets"`. Budgets can be weekly, monthly, or yearly — a period-specific title is misleading.

### 5. CategoryDrilldownScreen Navigation
- Added `onNavigateBack: () -> Unit` parameter with back arrow `IconButton` in `HeroPanel.navigationIcon`
- Added `onNavigateToCategoryDrilldown: () -> Unit` to `BudgetsScreen` with "View Categories" clickable text in HeroPanel content area
- Wired both in `MainActivity.kt` NavHost

### 6. AGENTS.md Compliance: Nested LazyColumn
Replaced `LazyColumn` inside `SheetList` in `CategoryDrilldownScreen` with `Column` + `forEach`. `SheetList` is a `Column`; nesting `LazyColumn` creates anti-pattern. Category lists are typically < 20 items — no performance benefit from lazy rendering.

### 7. FAB Color Fix
Changed FAB from `primary`/`onPrimary` to `primaryContainer`/`onPrimaryContainer`. On dark mode, `primary` is #FFFFFF (white FAB on white hero = invisible). `primaryContainer` provides tonal contrast.

### 8. Theme Token Compliance for Category Icons
Replaced `Color(android.graphics.Color.parseColor(budget.categoryColor))` with `MaterialTheme.colorScheme.primaryContainer` / `onPrimaryContainer`. Raw hex parsing bypasses design tokens and may produce colors that clash in dark mode.

### 9. Inline Amount Validation
Added `isAmountError` and `amountErrorText` computed from `amountText`. Passed to `SciuroTextField` via `isError` and `supportingText` parameters. Error message: "Enter a valid amount (1 – 100,000)".

### 10. Progress Bar Standardization
Both `BudgetsScreen` and `CategoryDrilldownScreen` now use `height(6.dp)` for `LinearProgressIndicator`.

## Consequences
- Create flow reduced from 4 taps to 2 taps (no confirmation dialog)
- Edit flow reduced from 4 taps to 2 taps
- All mutations provide immediate visual feedback via snackbar
- `CategoryDrilldownScreen` is now reachable and navigable
- FAB is visible in both light and dark mode
- Category icon backgrounds are theme-consistent
- Amount input shows inline validation errors
- Budget cards show period badges (Weekly/Monthly/Yearly)
- No more fake loading states (pull-to-refresh removed)
- ViewModel API surface changed: `createBudget`/`updateBudget`/`deleteBudget` are now `suspend fun`

## Files Changed
- `feature-budgets/.../ui/BudgetsScreen.kt` — major rewrite
- `feature-budgets/.../viewmodel/BudgetsViewModel.kt` — suspend functions, removed refresh
- `feature-budgets/.../ui/CategoryDrilldownScreen.kt` — rewritten
- `app/.../MainActivity.kt` — navigation wiring
