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

---

## Phase 2 — Loading & Navigation

### 2.1 Shimmer Skeleton Loading

**What was tested:**
- `SciuroSkeleton.kt` created with `shimmerEffect()` modifier (animated gradient via `drawBehind` + `InfiniteTransition`)
- Skeleton variants: `DashboardSkeleton` (hero placeholder + transaction rows), `BudgetCardSkeleton`, `AccountCardSkeleton`, `TransactionSkeletonRow`
- `DashboardState.isLoading` field added (default `true`, set to `false` after first combine emission)
- `DashboardScreen` shows `DashboardSkeleton()` when `state.isLoading` is true, else normal content
- Respects `reducedMotion()` — shimmer animation skipped when system setting active
- No new dependencies

**Compilation result:** PASS (core-ui, feature-dashboard)

**Manual test TODO:**
- [ ] Verify shimmer animation plays on cold app launch
- [ ] Verify shimmer transitions to real data once loaded
- [ ] Verify shimmer skips animation when reduced motion enabled

### 2.2 Predictive Back Gesture

**What was tested:**
- `OnBackInvokedCallback` registered in `MainActivity.onCreate()` on API 34+
- Manifest already has `android:enableOnBackInvokedCallback="true"`

**Compilation result:** PASS (app module compiles independently; blocked by pre-existing kanban error at full build)

### 2.3 Dynamic Color (Material You)

**What was tested:**
- `PalettePreference.DYNAMIC` added to enum in `Color.kt`
- `Theme.kt` calls `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` on API 31+ when DYNAMIC selected
- **No new dependency** — `dynamicLightColorScheme` already in extant `androidx.compose.material3:material3`
- SettingsUI: DYNAMIC shown only on API 31+ via `.filter { it != DYNAMIC || Build.VERSION.SDK_INT >= 31 }`
- String resource `palette_dynamic = "Dynamic (Material You)"` added to `feature-settings/strings.xml`

**Compilation result:** PASS (core-ui, feature-settings)

### Summary

| Test | Status |
|------|--------|
| Compilation (core-ui, feature-dashboard, feature-settings) | PASS |
| Shimmer skeleton infra | PASS (compile) |
| Dashboard skeleton integration | PASS (compile) |
| Predictive back callback | PASS (compile) |
| Dynamic Color (Material You) | PASS (compile) |
| String externalization | PASS |
| No new lint warnings | PASS |
