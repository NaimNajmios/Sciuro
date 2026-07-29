# TEST_NOTES — Developer Tools Pagination & HeroPanel Palette Inheritance

## What was tested

### HeroPanel Palette-Aware Colors
- **Manual visual verification** across all 6 palettes (Monochrome, Amber, Ocean, Forest, Plum, Slate) in both Light and Dark modes.
- HeroPanel background renders `MaterialTheme.colorScheme.primary` instead of `#000000` black.
- Title text, hero figure text, navigation icons use `MaterialTheme.colorScheme.onPrimary` — readable contrast maintained in all palette/mode combinations.
- WaveChart sparkline line + end circle use `onPrimary`; inner dot fill uses `primary`.
- PillToggle inside HeroPanel dynamically computes `isOnDarkSurface` from primary luminance.
- WalletScreen manual hero section (asset type selector) matches new palette-aware colors.
- All 14 HeroPanel call-sites across 7 feature modules compile without warnings.

### Ingestion Log Pagination
- Dead-letter events load in 50-item batches (matching Dashboard's `_transactionLimit` pattern).
- "Load More" button appears at list bottom when loaded < total matching rows.
- Clicking "Load More" increments limit by 50.
- Filter change (showRead toggle) resets limit to 50 to avoid stale offset.

### Mark as Read
- `markRead` marks a single dead-letter event as read.
- `markAllDeadLetterRead` marks all dead-letter events as read in one query.
- "Show read" toggle controls visibility — off (default) shows only unread events; on shows all.
- After marking all read, the list shows "No dead-letter events" (if empty) and can switch to "Show read" to confirm.
- The `is_read` column is `INTEGER NOT NULL DEFAULT 0` — existing rows default to unread (no migration data loss).

### Pipeline Trace Pagination
- Trace events load with configurable limit (default 100), increased by 100 per "Load More".
- Filter changes reset limit to 100.
- "Load More" button appears when loaded events >= 100 (simple heuristic for "more may exist").

## Build verification
- `:core-ui:compileReleaseKotlin` — passes
- `:core-ledger:generateCommonMainSciuroDatabaseInterface` — passes
- `:feature-settings:compileDebugKotlinAndroid` — passes
- `:feature-wallet:compileDebugKotlinAndroid` — passes
- `:feature-dashboard:compileDebugKotlinAndroid` — passes
- `:feature-debt:compileDebugKotlinAndroid` — passes
- `:feature-budgets:compileDebugKotlinAndroid` — passes
- `:feature-kanban:compileDebugKotlinAndroid` — passes

## Known issues
- None.
