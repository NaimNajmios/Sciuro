# Hero Panel Enhancement — Test Notes

## Scope
Add a `content` slot to `HeroPanel` and populate it across all 7 call sites with screen-specific context. Replace mock chart data on Dashboard with real balance history. Absorb the AccountDetailScreen's manually-wired "Adjust Balance" button into the slot.

## Changed files

| File | Change |
|---|---|
| `core-ui/.../HeroPanel.kt` | Added `content: @Composable ColumnScope.() -> Unit = {}` parameter, renders after chart section |
| `feature-dashboard/.../DashboardViewModel.kt` | Added `balanceHistory: List<Float>` to `DashboardState`, computed from daily-aggregated running balance via `computeBalanceHistory()` |
| `feature-dashboard/.../DashboardScreen.kt` | Replaced mock chart data with real `balanceHistory` filtered by toggle range; added accounts count + adjustments secondary row in `content` slot |
| `feature-budgets/.../BudgetsScreen.kt` | `heroFigure` changed from count to `"RM X / RM Y"` (total spent vs allocated); `content` slot shows top 3 at-risk budgets by progress |
| `feature-wallet/.../AccountDetailScreen.kt` | Removed outer `Column(background(SurfaceHero))` wrapper; moved "Adjust Balance" button into `content` slot; cleaned up unused `SurfaceHero` import |
| `feature-settings/.../SettingsViewModel.kt` | Added `_lastCapturedAt` StateFlow fetched via `rawEventRepository.getLastCapturedAt()` in `refreshCounts()` |
| `feature-settings/.../DeveloperSettingsScreen.kt` | `heroFigure` changed from `"Tools"` to relative time since last capture; `content` slot shows pending/dead-letter counts |
| `feature-kanban/.../KanbanScreen.kt` | `content` slot shows Upcoming/Due/Settled task counts from actual status distribution |

## Verification
- `./gradlew :core-ui:compileDebugKotlin` — passes
- `./gradlew :feature-dashboard:compileDebugKotlinAndroid` — passes
- `./gradlew :feature-budgets:compileDebugKotlinAndroid` — passes
- `./gradlew :feature-wallet:compileDebugKotlinAndroid` — passes
- `./gradlew :feature-settings:compileDebugKotlinAndroid` — passes
- `./gradlew :feature-kanban:compileDebugKotlinAndroid` — passes

All 7 existing call sites compile without changes. The `content` default of `{}` is a no-op, so every existing screen renders identically until a call site opts in.
