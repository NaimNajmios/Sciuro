# Phase K2 — UX Polish Test Notes

## Phase 1 — Tactile Polish

### 1.1 Unified Haptic Architecture

**What was tested:**
- All 4 modules (`core-ui`, `feature-dashboard`, `feature-budgets`, `feature-debt`) compile cleanly
- `SciuroHaptics.selection()` / `success()` / `warning()` / `error()` / `transferMatch()` all take `HapticFeedback` parameter
- `InteractiveModifier.bounceClick()` uses `SciuroHaptics.selection()` via captured `LocalHapticFeedback.current`
- `SciuroPrimaryButton` uses `SciuroHaptics.success()`
- `FastTransactionSheet` FilterChips and Numpad buttons use `SciuroHaptics.selection()`
- No double-haptic on save button (removed from Numpad's SciuroPrimaryButton onClick since the button itself handles it)

**Compilation result:** PASS (all modules compile)

**Manual test TODO:**
- [ ] Verify haptic fires on numpad keypress (selection tick)
- [ ] Verify haptic fires on primary button press (success)
- [ ] Verify haptic fires on bounceClick (card tap)
- [ ] Verify haptic fires on pull-to-refresh release
- [ ] Verify haptic fires on swipe-to-approve / swipe-to-reject

### 1.2 Long-Press Context Menus

**What was tested:**
- `TransactionCard` now accepts optional `onLongClick` parameter
- `TransactionList` provides `DropdownMenu` with "Copy amount" and "Mark as transfer" on long-press
- `BudgetsScreen` provides `DropdownMenu` with "Edit budget", "View transactions", "Delete budget" on long-press
- `DebtOverviewScreen` provides `DropdownMenu` with "Edit debt", "Mark as finished", "Delete debt" on long-press
- Context menu strings externalized in respective `strings.xml` files

**Compilation result:** PASS (all modified modules compile)

**Manual test TODO:**
- [ ] Long-press TransactionCard → menu appears with correct options
- [ ] "Copy amount" copies formatted amount to clipboard
- [ ] Long-press budget card → menu appears
- [ ] Menu items trigger correct actions (edit/view/delete)
- [ ] Long-press debt card → menu appears
- [ ] Dismissing menu by tapping outside works

### 1.3 Edge-to-Edge System Bar Theming

**What was tested:**
- `HeroPanel` top padding now uses dynamic `WindowInsets.statusBars` + 24.dp (instead of fixed 48.dp)
- `DashboardScreen` uses `rememberLazyListState` to track scroll position
- `SideEffect` sets `window.statusBarColor` based on scroll position:
  - At top (HeroPanel visible) → primary color
  - Scrolled past HeroPanel → surface color
- Status bar icon color adapts via `WindowInsetsControllerCompat.isAppearanceLightStatusBars`
- Uses `derivedStateOf` to avoid unnecessary recompositions

**Compilation result:** PASS

**Manual test TODO:**
- [ ] Verify HeroPanel background bleeds full-bleed behind status bar
- [ ] Verify status bar is transparent/primary when at top of scroll
- [ ] Verify status bar transitions to surface color when scrolled down
- [ ] Verify status bar icons are visible (light on dark HeroPanel, dark on light surface)
- [ ] Verify reduced-motion does not affect this (only color changes, no animation)

### Pre-existing Issues

- `feature-kanban` has unresolved references (`transferCandidateIds`, `driftedAmounts`) — pre-existing, not introduced by this phase

### Summary

| Test | Status |
|------|--------|
| Compilation (all changed modules) | PASS |
| Haptic architecture | PASS (compile) |
| Long-press context menus | PASS (compile) |
| Edge-to-edge theming | PASS (compile) |
| String externalization | PASS |
| No new lint warnings | PASS |
