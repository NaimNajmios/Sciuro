# Test Notes: Phase B7.5 - Budget Feature Completion

## Scope
Closed the gap between the scaffolded B7/C3 budget skeleton and a working feature:

### Budget-1: Repository & Category Resolution
- Added `updateBudget` and `deleteBudget` queries to `Budget.sq` (SQLDelight).
- Added `updateBudget()` and `deleteBudget()` methods to `BudgetRepository` following the existing `withAudit` pattern.
- Fixed category name resolution in `BudgetsViewModel`: injected `CategoryRepository`, built in-memory `categoryMap` via `combine` on `observeBudgets()` + `observeCategoriesByType("OUTFLOW")`, replacing the raw `category_id` string.

### Budget-2: Creation, Edit & Delete UI Flows
- Empty state CTA now opens the creation sheet (was a no-op).
- FAB "+" button matching the Dashboard pattern for adding budgets.
- Creation bottom sheet with:
  - `FilterChip` row for expense category selection.
  - `SciuroTextField` for amount with decimal keyboard.
  - `SegmentedButton` row for period (WEEKLY/MONTHLY/YEARLY).
  - "Create Budget" button, disabled until valid input.
- Edit sheet (tap card): pre-filled, Save + Delete buttons.
- Delete confirmation via `SciuroConfirmationDialog` with destructive styling.
- ViewModel methods: `createBudget()`, `updateBudget()`, `deleteBudget()`.

## Files Changed
- `core-ledger/.../db/Budget.sq` — added `updateBudget`, `deleteBudget`, `selectBudgetById` queries.
- `core-budget/.../BudgetRepository.kt` — added `updateBudget()`, `deleteBudget()` with audit logging.
- `feature-budgets/.../BudgetsViewModel.kt` — added `CategoryRepository` injection, category name resolution, CRUD methods.
- `feature-budgets/.../BudgetsModule.kt` — updated Koin wiring.
- `feature-budgets/.../BudgetsScreen.kt` — full rewrite with creation/edit/delete bottom sheets and FAB.

## Results
- `assembleDebug` builds successfully (`BUILD SUCCESSFUL`).
- Budget cards now display category names (e.g., "Food & Beverage") instead of raw UUIDs.
- Users can create, edit, and delete budgets from the UI.
- All mutations are audited via `AuditableRepository.withAudit`.

## Excluded
- Model extension (`rollover`, `alertThresholdPercent`) — deferred to Budget-3.
- Three-state visual system (healthy/approaching/over) — deferred to Budget-3.
- Transfer-exclusion bug fix in `BudgetEngine` — deferred to Budget-4.
- Reactive `BudgetEngine` wiring into transaction paths — deferred to Budget-5.
- Category drilldown screen — deferred to Budget-6.
- Auto-suggested limits and threshold-crossing cascade — deferred to Budget-6.
