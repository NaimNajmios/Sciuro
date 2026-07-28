# Phase J2 — Developer Options & Settings Redesign — Test Notes

## Scope

Redesign the UI structure and layout of all developer options tabs and the main Settings screen, following the project's full-screen swiping pattern (Box + LazyColumn + HeroPanel + SheetList).

**Developer Options (7 tabs):**
- Navigation: ScrollableTabRow → PillToggle(scrollable=true)
- Health tab: SciuroCard wrapper with summary row (packages, processed, avg match rate), SciuroCard for pipeline metrics
- Data Tools tab: Added database stats panel (pending + dead letter counts) above Clear Inbox button
- Ingestion Log tab: Unified Pending/Dead Letter counts in single SciuroCard, refresh button, SciuroCard for dead-letter events
- Sources tab: Each category (Banks, Aggregators, Custom) in SciuroCard, kept CRUD (add/remove/edit)
- Sources edit: New `renamePackage()` method in MutableIngestionAllowlist, edit icon + SciuroFormSheet in UI
- Simulator tab: Batch Test Runner uses SciuroCard, SimulationResult inlined with colored indicator strip
- Diagnostics tab: Input form wrapped in SciuroCard, RoundedCornerShape(16.dp) on all semantic-colored cards
- Pipeline Trace tab: Filter section wrapped in SciuroCard, RoundedCornerShape(12.dp) on detail trace cards

**Settings Screen:**
- Root structure: Column → Box(fillMaxSize) + LazyColumn following AGENTS.md pattern
- HeroPanel inside LazyColumn item (scrolls with content instead of fixed at top)
- SheetList uses fillParentMaxHeight() instead of weight(1f); eliminated nested LazyColumn
- importFilePickerLauncher moved from LazyColumn item to top-level composable scope
- Battery state: replaced stale remember {} with var + LaunchedEffect (recomputes on screen re-entry)
- Notifications section (330 lines) extracted into NotificationsSection(uiState, viewModel) composable
- showBackupConfig / showLargeTxnConfig / showDebtDueConfig states localized inside NotificationsSection
- HeroPanel heroFigure shows "Sciuro" instead of duplicating "Settings" title
- showExportDialog / importFileUri moved to top-level scope
- Unused BrandPrimaryDark import removed

## Verification

| Command | Result | Notes |
|---------|--------|-------|
| `./gradlew :feature-settings:compileDebugKotlinAndroid --no-build-cache --rerun-tasks` | PASS | 0 new warnings, 47 tasks executed |
| `./gradlew :core-ingestion:compileDebugKotlinAndroid` | PASS | MutableIngestionAllowlist compiles |
| `./gradlew assembleDebug` | PASS | Full APK build, 512 tasks, 0 new warnings |

## Files Changed

### Developer Options Tabs (committed in HEAD~1)

| File | Change |
|------|--------|
| `feature-settings/.../ui/DeveloperSettingsScreen.kt` | ScrollableTabRow → PillToggle(scrollable=true) |
| `feature-settings/.../ui/DeveloperTabHealth.kt` | SciuroCard wrapper + summary row (packages/processed/avg match rate), SciuroCard for pipeline metrics |
| `feature-settings/.../ui/DeveloperTabDataTools.kt` | Added database stats panel (pending + dead letter counts) inside SciuroCard |
| `feature-settings/.../ui/DeveloperTabIngestionLog.kt` | Unified stats in single SciuroCard, refresh IconButton, SciuroCard for dead-letter events |
| `feature-settings/.../ui/DeveloperTabSources.kt` | Category groups in SciuroCard, edit icon + SciuroFormSheet for rename, kept CRUD |
| `feature-settings/.../ui/DeveloperTabSimulator.kt` | Batch Test Runner in SciuroCard, SimulationResult inlined with colored indicator strip, removed standalone composable |
| `feature-settings/.../ui/DeveloperTabDiagnostics.kt` | Input form wrapped in SciuroCard, RoundedCornerShape(16.dp) on semantic-colored cards |
| `feature-settings/.../ui/DeveloperTabPipelineTrace.kt` | Filter section in SciuroCard, RoundedCornerShape(12.dp) on detail cards |
| `core-ingestion/.../config/MutableIngestionAllowlist.kt` | Added renamePackage(oldName, newName) method |
| `feature-settings/.../res/values/strings.xml` | Added dev_sources_edit_title, dev_sources_edit_label, dev_sources_edit_action |

### Settings Screen (uncommitted)

| File | Change |
|------|--------|
| `feature-settings/.../ui/SettingsScreen.kt` | Box/LazyColumn pattern, SheetList with fillParentMaxHeight, NotificationsSection extracted, launcher scoping fix, battery lifecycle fix |

## Issues Encountered

None. All tabs compile and the full APK builds cleanly with zero new warnings.
