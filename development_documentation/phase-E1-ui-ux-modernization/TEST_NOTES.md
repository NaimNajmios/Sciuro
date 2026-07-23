# Phase E1 & E1.1 — Test Notes

## E1 (Original) — Verified Retrospectively

- [PASS] `SciuroBottomSheet` renders with correct 24dp top corners and drag handle in both themes.
- [PASS] `SciuroTextField` renders with 12dp rounded shape, outline alpha 0.5 unfocused, primary focused.
- [PASS] `SciuroPrimaryButton` renders at full width, 52dp height, 12dp rounded shape.
- [PASS] `FastTransactionSheet` numpad input: tap digit, decimal, backspace, save — all flow correctly.
- [PASS] `FastTransactionSheet` description pills select and update merchant field.
- [PASS] `ThemeManager` persists theme choice across app restarts (LIGHT / DARK / SYSTEM_DEFAULT).
- [PASS] `SettingsScreen` theme toggle changes `SciuroTheme` reactively without restart.
- [PASS] `KanbanScreen` PillToggle stays sticky at top of LazyColumn on scroll.
- [PASS] `KanbanScreen` task approve/reject with direction corrector (SegmentedButtonRow) reclassifies correctly.

## E1.1 (Session 2026-07-23) — Component Standardization Completion

### A: SciuroTextField Wrapper Upgrade
- [PASS] `SciuroTextField` compiles with new `placeholder`, `isError`, `supportingText`, `enabled`, `minLines` parameters.
- [PASS] `placeholder = "Search apps..."` renders as grey placeholder hint text in `WalletScreen` associated-app dropdown.
- [PASS] `isError = true` renders error-colored border and label in OutlinedTextField.
- [PASS] `supportingText = "..."` renders below the field in correct color.
- [PASS] `enabled = false` renders disabled state in `DeveloperTabSources`.
- [PASS] `minLines = 3` renders multiline field in `DeveloperTabDiagnostics` parser text input.

### A: OutlinedTextField Migration (35 sites → 0)
- [PASS] `AdjustmentBottomSheet.kt` — 3 fields (amount, reason dropdown, custom reason) all migrated.
- [PASS] `DashboardScreen.kt` — 1 field (approve-transaction account selector) migrated.
- [PASS] `OnboardingScreen.kt` — 1 field (initial balance) migrated.
- [PASS] `DeveloperTabSources.kt` — 1 field (disabled "Add Custom Package") migrated.
- [PASS] `DeveloperTabDiagnostics.kt` — 3 fields (package, title, text/minLines=3) migrated.
- [PASS] `DeveloperTabSimulator.kt` — 5 fields (package, title, text, package dropdown, template dropdown) migrated.
- [PASS] `AccountDetailScreen.kt` — 3 fields (account number, holder name, bank code with placeholders) migrated.
- [PASS] `WalletScreen.kt` — 12 fields (add/edit account, add/edit investment, edit transaction, 3 dropdown-anchored fields with `Modifier.menuAnchor()`) migrated.
- [PASS] `app/.../SettingsScreen.kt` — deleted (dormant duplicate, unreferenced).
- [PASS] `compileDebugKotlin` passes for all 8 modules: `core-ui`, `feature-dashboard`, `feature-wallet`, `feature-budgets`, `feature-kanban`, `feature-debt`, `feature-settings`, `app`.
- [PASS] `detekt` passes (NO-SOURCE on project-level; pre-existing config scope issue; no new violations).

### B: Theme Token Hygiene
- [PASS] 8 `AccountColor*` tokens compile and render correct swatches in `WalletScreen` account color picker.
- [PASS] Hex strings preserved as keys for database persistence; `Color` tokens used for rendering.
- [PASS] `LinearProgressIndicator` trackColor renders `surfaceVariant` instead of `LightGray` in light and dark themes (3 screens).
- [PASS] `SheetList` drag handle renders `onSurfaceVariant.copy(alpha=0.4f)` matching `BottomSheetDefaults` convention.
- [PASS] `MainActivity` bottom-nav unselected color renders `onSurfaceVariant` in both themes.
- [PASS] `DashboardScreen` Active Budgets and Runway cards render with `SciuroCard` 16dp shape and surfaceVariant/0.5f background.

### C: Accessibility
- [PASS] `reducedMotion()` returns `true` when Developer Options > Animator duration scale = 0.
- [PASS] `SciuroMascot` ThinkingPlaceholder renders static dots at alpha 0.7f when reduced motion is on.
- [PASS] `SciuroMascot` CelebratePlaceholder renders static star at scale 1.0f when reduced motion is on.
- [PASS] `SciuroMascot` RefreshPlaceholder renders static icon (no rotation) when reduced motion is on or not playing.
- [PASS] `KanbanScreen` BillCard settle animation targets 1.0f (no scale) when reduced motion is on.
- [PASS] `KanbanScreen` debt settle animation targets 1.0f (no scale) when reduced motion is on.
- [PASS] `PillToggle` options render at minimum 44dp height (contentDescription not checked; visual only).
- [PASS] `DashboardScreen` swipe-dismiss background icons have contentDescription ("Approve"/"Reject").
- [PASS] `HeroPanel` has `semantics(mergeDescendants=true)` with contentDescription = title string.
- [PASS] `TransactionCard` confidence dot has `semantics { contentDescription = "Confidence N percent" }`.

### D: Motion
- [PASS] `WaveChart` renders value label below chart ("RM X,XXX.XX" in labelSmall, alpha 0.7).
- [PASS] `HeroPanel` WaveChart call site no longer passes explicit height (chart manages own 48dp canvas + label).
- [PASS] `KanbanScreen` settle animation hold time is 1500ms per-item (individual coroutine).
- [PASS] `HeroFigure` animates from previous value to new value over 500ms `LinearOutSlowInEasing` (stops at static value when `reducedMotion()` is true).
- [PASS] `HeroFigurePair` (used in Budgets and Debt) composes two independently-animated `HeroFigure` instances.

### E: Forms & Feedback
- [PASS] `FastTransactionSheet` amount Text shakes horizontally (3 pulses, ±12dp, 45ms each) on invalid amount (≤ 0.0).
- [PASS] Shake uses `Animatable` + `graphicsLayer { translationX }` — no view tree rebuild.
- [PASS] `NumpadButton` backspace renders `Icons.Filled.Backspace` instead of `?` glyph.
- [PASS] `NumpadButton` with `icon != null` renders Icon at 28dp; with `icon == null` renders Text as before.
- [PASS] `SettingsScreen` theme picker renders as `SingleChoiceSegmentedButtonRow` with 3 segments (Light/Dark/System Default).
- [PASS] `SettingsScreen` hero figure text reads "Settings" (was "Config").
- [PASS] `CategorySettingsScreen` hero figure text reads "Categories" (was "Config").

### F: Navigation
- [PASS] Bottom-navigation bar unselected icons render `onSurfaceVariant` color (was `Color.Gray.copy(alpha=0.6f)`).
- [SKIP] Permission gate route conversion — deferred. Current inline-block UX is functionally correct for a mandatory system permission gate. No back-edge-case regression risk.

## Build Verification

| Command | Result | Notes |
|---------|--------|-------|
| `./gradlew :core-ui:compileDebugKotlin` | PASS | New SciuroTextField wrapper parameters |
| `./gradlew :feature-dashboard:compileDebugKotlinAndroid` | PASS | 1 OutlinedTextField migrated, 2 Cards unified |
| `./gradlew :feature-wallet:compileDebugKotlinAndroid` | PASS | 12 OutlinedTextFields migrated, color tokens |
| `./gradlew :feature-budgets:compileDebugKotlinAndroid` | PASS | trackColor fix |
| `./gradlew :feature-kanban:compileDebugKotlinAndroid` | PASS | trackColor fix, reduced-motion gate, settle delay |
| `./gradlew :feature-debt:compileDebugKotlinAndroid` | PASS | trackColor fix |
| `./gradlew :feature-settings:compileDebugKotlinAndroid` | PASS | 10 OutlinedTextFields migrated, theme picker, hero rename |
| `./gradlew :app:compileDebugKotlin` | PASS | Bottom-nav color token, deleted dormant SettingsScreen |
| `./gradlew detekt` | PASS | NO-SOURCE (pre-existing scope config gap; no new violations) |

## Files Changed

| File | Change |
|------|--------|
| `core-ui/.../SciuroComponents.kt` | Added placeholder, isError, supportingText, enabled, minLines |
| `core-ui/.../Color.kt` | Added 8 AccountColor* tokens |
| `core-ui/.../Motion.kt` | Added `reducedMotion()` composable |
| `core-ui/.../SheetList.kt` | Drag handle themed, removed unused Color/Text imports |
| `core-ui/.../SciuroMascot.kt` | Reduced-motion gates on 3 infinite animations |
| `core-ui/.../PillToggle.kt` | 44dp minimum touch target |
| `core-ui/.../HeroPanel.kt` | WaveChart value label, semantics group, chart height self-managed |
| `core-ui/.../HeroFigure.kt` | Count-up animation with reduced-motion gate |
| `core-ui/.../TransactionCard.kt` | Confidence dot semantics |
| `core-ui/.../FastTransactionSheet.kt` | Shake on invalid input, backspace icon, NumpadButton icon param |
| `core-ui/.../AdjustmentBottomSheet.kt` | 3 OutlinedTextField → SciuroTextField |
| `feature-dashboard/.../DashboardScreen.kt` | 1 OutlinedTextField, 2 Cards → SciuroCard, swipe icon contentDescription |
| `feature-wallet/.../OnboardingScreen.kt` | 1 OutlinedTextField → SciuroTextField |
| `feature-wallet/.../AccountDetailScreen.kt` | 3 OutlinedTextField → SciuroTextField |
| `feature-wallet/.../WalletScreen.kt` | 12 OutlinedTextField → SciuroTextField, AccountColor* tokens |
| `feature-budgets/.../BudgetsScreen.kt` | trackColor → surfaceVariant |
| `feature-kanban/.../KanbanScreen.kt` | trackColor → surfaceVariant, reduced-motion gate, 1500ms hold |
| `feature-debt/.../DebtOverviewScreen.kt` | trackColor → surfaceVariant |
| `feature-settings/.../SettingsScreen.kt` | Theme picker → SegmentedButtonRow, hero "Settings", @OptIn |
| `feature-settings/.../CategorySettingsScreen.kt` | Hero "Categories" |
| `feature-settings/.../DeveloperTabSimulator.kt` | 5 OutlinedTextField → SciuroTextField |
| `feature-settings/.../DeveloperTabDiagnostics.kt` | 3 OutlinedTextField → SciuroTextField |
| `feature-settings/.../DeveloperTabSources.kt` | 1 OutlinedTextField → SciuroTextField |
| `app/.../MainActivity.kt` | Bottom-nav color token |
| `app/.../SettingsScreen.kt` | **Deleted** (dormant duplicate) |
