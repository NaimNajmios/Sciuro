# Phase K2 — UX Polish (State-of-the-Art Enhancements)

## Overview

Elevate Sciuro from solid production grade to award-tier polish through micro-interactions, contextual intelligence, platform-native depth, and intelligent loading states. **All 4 phases are now complete.**

## Phase 1 — Tactile Polish ✅

| # | Enhancement | Files | Effort |
|---|------------|-------|--------|
| 1.1 | Unified Haptic Architecture | `SciuroHaptics.kt` (new), `InteractiveModifier.kt`, `SciuroComponents.kt`, `FastTransactionSheet.kt`, `DashboardScreen.kt` | Small |
| 1.2 | Long-Press Context Menus | `TransactionCard.kt`, `TransactionList.kt`, `BudgetsScreen.kt`, `DebtOverviewScreen.kt` | Small |
| 1.3 | Edge-to-Edge System Bar Theming | `DashboardScreen.kt`, `HeroPanel.kt` | Medium |

**No new dependencies.**

### 1.1 Haptics
- New `SciuroHaptics` object wrapping `LocalHapticFeedback` with semantic methods: `selection()`, `success()`, `warning()`, `error()`, `transferMatch()`
- Replace all raw `performHapticFeedback` calls across the codebase
- Reuse existing `bounceClick` pattern (already using `LocalHapticFeedback`)

### 1.2 Long-Press Menus
- Extend `bounceClick` modifier with optional `onLongClick` parameter
- TransactionCard: "Copy amount", "Create obligation from this", "Mark as transfer", "Hide from budget"
- Budget cards: "Edit budget", "View transactions", "Pause budget"
- Debt cards: "Mark as paid", "Edit debt", "View linked transactions"

### 1.3 Edge-to-Edge
- Scroll-aware status bar: transparent when HeroPanel visible, surface color when scrolled
- HeroPanel receives `statusBarsPadding()` for safe content placement while background bleeds full-bleed

## Phase 2 — Loading & Navigation ✅

| # | Enhancement | Files | Effort |
|---|------------|-------|--------|
| 2.1 | Shimmer Skeleton Loading | `SciuroSkeleton.kt` (new), all screen files | Medium |
| 2.2 | Predictive Back Gesture | `MainActivity.kt`, `Motion.kt` | Small |
| 2.3 | Dynamic Color (Material You) | `Theme.kt`, `Color.kt`, SettingsThemeScreen | Medium |

### 2.1 Shimmer
- `SciuroSkeleton` composable with reusable `Modifier.shimmerEffect()` extension
- Variants: `TransactionSkeletonRow`, `DashboardSkeleton`, `BudgetCardSkeleton`, `AccountCardSkeleton`
- Integrate with `UiState.Loading` branch in each screen
- Respects `reducedMotion()` — skips animation when system setting active

### 2.2 Predictive Back
- Register `OnBackInvokedCallback` (API 34+) in `MainActivity`
- Manifest already has `enableOnBackInvokedCallback="true"`
- Defer full peek animation until Compose BOM upgrade; initial implementation prevents unexpected exits

### 2.3 Dynamic Color
- No new dependency — `dynamicLightColorScheme`/`dynamicDarkColorScheme` already in `material3` artifact
- Add `PalettePreference.DYNAMIC` enum value
- Conditional call on API 31+ in `SciuroTheme`

## Phase 3 — Security & Reach ✅

| # | Enhancement | Files | Effort |
|---|------------|-------|--------|
| 3.1 | Biometric for Destructive Actions | `BiometricConfirmDialog.kt` (new), all screens with destructive dialogs | Small |
| 3.2 | Smart Reply / Inline Notification Actions | `NotificationHelper.kt`, `NotificationActionReceiver.kt` (new), `AndroidManifest.xml` | Medium |
| 3.3 | Smart Widgets (Glance) | `:feature-widget` module (new), `libs.versions.toml`, `settings.gradle.kts` | Large |

**New dependencies:** `androidx.glance:glance-appwidget` + `glance-material3`

### 3.1 Biometric
- Reusable `BiometricConfirmDialog` extending existing `BiometricGate` pattern
- Gate destructive actions: delete transaction, delete account, clear all data, change API key

### 3.2 Notifications
- `NotificationActionReceiver` BroadcastReceiver for action intents
- Transfer review: "Yes, it's me" / "Not me" actions
- Bill reminder: "Mark as paid" / "Snooze 1 day" actions
- Uses KoinComponent for repository access in receiver

### 3.3 Widgets
- New `:feature-widget` Android library module
- `SciuroBalanceWidget`: net position + "Add Transaction" action
- `SciuroBudgetWidget`: top 3 at-risk categories with progress bars
- Glance uses separate composable tree (`@GlanceComposable`), different State pattern

## Phase 4 — Intelligence ✅

| # | Enhancement | Files | Effort |
|---|------------|-------|--------|
| 4.1 | Predictive Analytics Cards | `RunwayPredictor.kt` (new, core-budget), `PredictionCard.kt` (new), `Sparkline.kt` (new, core-ui), `DashboardViewModel.kt` | Large |
| 4.2 | Per-Account Spending Velocity | `VelocityCalculator.kt` (new, core-ledger), `AccountVelocityCard.kt` (new), `AccountDetailScreen.kt` | Large |

**No new dependencies.** KMP logic in existing core-* modules.

---

## Phase 5 — Future / Under Consideration

- **Wallpaper-Based Account Colors**: Extract dominant/vibrant colors from device wallpaper to auto-assign account card colors
- **Shared Element Transitions**: Animate card expansion from list to detail screen
- **Cross-Device Sync**: Push-based sync between devices via Ktor backend
- **Widget Tap Actions**: Wire `actionStartActivity<MainActivity>()` once `:feature-widget` can depend on `:app` module pattern is established

---

## Architecture Compliance

All enhancements follow existing constraints:
- **Koin DI** for all new ViewModels
- **core-ui primitives** only for new components
- **StateFlow\<UiState\>** pattern
- **Full-screen swiping** Box+LazyColumn layout
- **SCI-prefixed** color tokens only
- **strings.xml** externalization
- **No cross-feature imports**; Domain Event Bus for cross-module communication
- **No hardcoded versions**; all in `libs.versions.toml`

## Test Coverage

- Verify `SciuroHaptics` compiles and all call sites resolve
- Verify long-press menus render and dismiss correctly
- Verify scroll-aware status bar transitions on dashboard
- See `TEST_NOTES.md` for detailed test results
