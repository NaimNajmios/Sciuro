# UI Fixes Implementation

## 1. HeroPanel — Add IBMPlexMono font to heroFigure

**File:** `core-ui/src/main/java/com/najmi/sciuro/core/ui/components/HeroPanel.kt`

Changes:
- Add import: `import com.najmi.sciuro.core.ui.theme.IBMPlexMono`
- Line 52: Add `fontFamily = IBMPlexMono` to the heroFigure Text composable

## 2. WalletScreen — Replace inline hero + fix layout glitches

**File:** `feature-wallet/src/androidMain/kotlin/com/sciuro/feature/wallet/ui/WalletScreen.kt`

Changes:
- Lines 127-163: Replace inline Column hero (with manual SurfaceHero background, displaySmall, inline PillToggle) with the shared HeroPanel composable
- Remove the standalone PillToggle (now inside HeroPanel)
- Line 287: Change `LazyColumn` modifier from `.weight(1f)` to `.fillMaxSize()`
- Line 303: Fix `items(accountTx.take(20).size) { idx -> val tx = accountTx[idx] }` to `items(accountTx.take(20)) { tx -> }`
- Lines 354-360: Collapse 3 dead if/else branches into single `EmptyStateView("No investment transactions yet.")`
- Add bottom padding (80.dp) to transaction list for FAB

## 3. MainActivity — Active tab indicator

**File:** `app/src/main/java/com/najmi/sciuro/MainActivity.kt`

Changes:
- Add imports for `Icons.Outlined.*` variants (Home, List, ShoppingCart, CheckCircle, Settings)
- Line 83-88: Change items list to hold both filled and outlined icons per route
- Lines 155-168: Customize NavigationBarItem:
  - Use `Icons.Filled.*` when selected, `Icons.Outlined.*` when not
  - Add `FontWeight.Bold` for active label, `FontWeight.Normal` for inactive
  - Add custom `indicator` with a `RoundedCornerShape` background for active tab
  - Customize colors via `NavigationBarItemDefaults.colors()`

## 4. KanbanScreen — Stretch PillToggle to full width

**File:** `feature-kanban/src/androidMain/kotlin/com/sciuro/feature/kanban/ui/KanbanScreen.kt`

Changes:
- Line 71: Add `modifier = Modifier.fillMaxWidth()` to PillToggle call

## 5. DashboardScreen — EmptyStateView for empty transactions

**File:** `feature-dashboard/src/androidMain/kotlin/com/sciuro/feature/dashboard/ui/DashboardScreen.kt`

Changes:
- Lines 144-145: Replace `Text("No transactions found"...)` with `EmptyStateView(message = "No transactions found")`

## 6. SettingsScreen — HeroPanel + SheetList redesign

**File:** `feature-settings/src/androidMain/kotlin/com/sciuro/feature/settings/ui/SettingsScreen.kt`

Changes:
- Remove Scaffold + TopAppBar wrapper
- Wrap content in Column(fillMaxSize) -> HeroPanel(title="Settings", heroFigure="") -> SheetList(offset(-24.dp).fillParentMaxHeight()) -> scrollable Column with content
- Keep all business logic: theme chips, LLM toggle, API key input, test connection button, developer options card
- Add imports for HeroPanel, SheetList, SurfaceHero
