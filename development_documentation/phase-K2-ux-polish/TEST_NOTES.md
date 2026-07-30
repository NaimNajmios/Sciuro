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

## Phase 4 — Intelligence

### 4.1 Predictive Analytics Cards

**What was tested:**
- `RunwayPredictor.kt` in `core-budget/engine/` — KMP prediction engine computing 30-day forward projection.
- `Sparkline.kt` in `core-ui/components/` — reusable Canvas sparkline with configurable `lineColor`.
- `PredictionCard.kt` in `feature-dashboard/components/` — color-coded runway days + sparkline + confidence.
- `DashboardViewModel` — injects `RunwayPredictor`, computes `RunwayPrediction` in combine.
- RunwayPredictor registered as Koin singleton in `BudgetModule`.
- DashboardModule updated with 13th constructor parameter.

**Compilation result:** PASS (core-budget, core-ui, feature-dashboard)

### 4.2 Per-Account Spending Velocity

**What was tested:**
- `VelocityCalculator.kt` in `core-ledger/engine/` — KMP calculator for daily average, burn rate, trend.
- `AccountVelocityCard.kt` in `feature-wallet/components/` — color-coded burn progress bar, trend arrow.
- `AccountDetailViewModel` — adds `SpendingVelocity` to state, computes from transactions.
- `AccountDetailScreen` — velocity card before Transaction History header.
- 3 new strings in `feature-wallet/strings.xml`.

**Compilation result:** PASS (core-ledger, feature-wallet)

### 3.3 Smart Widgets (Glance)

**What was tested:**
- New `:feature-widget` module with `glance-appwidget:1.1.1` + `glance-material3:1.1.1`.
- `SciuroBalanceWidget` — shows net position from all accounts, 30-min auto-refresh.
- `SciuroBudgetWidget` — shows top 3 at-risk budgets with progress percentages.
- Widget XML metadata, empty WidgetModule Koin module.
- Added to `settings.gradle.kts`, `libs.versions.toml`, `SciuroApp.kt`.

**Compilation result:** PASS

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

### 2.2 Predictive Back Gesture

**What was tested:**
- `OnBackInvokedCallback` registered in `MainActivity.onCreate()` on API 34+
- Manifest already has `android:enableOnBackInvokedCallback="true"`

**Compilation result:** PASS (app module)

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

---

## Phase 3 — Security & Reach

### 3.1 Biometric Confirmation for Destructive Actions

**What was tested:**
- `BiometricConfirmDialog.kt` created in `core-ui/components/` — reusable composable triggering `BiometricPrompt` on compose
- Uses same `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` pattern as existing `BiometricGate`
- Falls back to direct `onConfirmed()` if device has no biometric/credential enrolled
- `androidx.biometric` dependency added to `core-ui/build.gradle.kts`
- Integrated into 3 destructive paths:
  - `DashboardScreen`: delete transaction — confirm dialog triggers biometric, then executes
  - `BudgetsScreen`: delete budget — same pattern
  - `DebtOverviewScreen`: delete debt — same pattern

**Compilation result:** PASS (core-ui, feature-dashboard, feature-budgets, feature-debt)

### 3.2 Smart Reply / Inline Notification Actions

**What was tested:**
- `NotificationActionReceiver.kt` created in `app/receiver/` — `BroadcastReceiver` with `KoinComponent` for DI
- Handles 4 actions: `APPROVE_TRANSFER`, `REJECT_TRANSFER`, `MARK_PAID`, `SNOOZE_BILL`
- `NotificationHelper.showTransferReviewAlert()` — added "Yes, it's me" and "Not me" inline actions
- `NotificationHelper.showBillReminder()` — added "Mark as paid" and "Snooze 1 day" inline actions
- Receiver registered in `AndroidManifest.xml` as `android:exported="false"`

**Compilation result:** PASS (app module)

### Summary

| Test | Status |
|------|--------|
| Compilation (core-ui, feature-dashboard, feature-budgets, feature-debt, app) | PASS |
| BiometricConfirmDialog | PASS (compile) |
| NotificationActionReceiver | PASS (compile) |
| NotificationHelper actions | PASS (compile) |
| AndroidManifest registration | PASS |
| No new lint warnings | PASS |
