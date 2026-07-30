# Phase K3 — Scroll Fix & Dashboard Transaction Display

## Issues Fixed

### 1. Account Detail Screen — Cannot Scroll

**Root cause:** `AccountDetailScreen.kt` used a root `Column(modifier = Modifier.fillMaxSize())` with no scrollable container. The `SheetList` used `weight(1f)` instead of `fillParentMaxHeight()`, and the inner `Column` lacked `verticalScroll`. The AGENTS.md full-screen swiping pattern (`Box > LazyColumn > item{HeroPanel} + item{SheetList}`) was not followed.

**Fix:** Restructured the root layout:
- Replaced `Column(fillMaxSize)` with `Box(fillMaxSize)` wrapping a `LazyColumn`
- `HeroPanel` + dropdown menu moved into `item { }` block
- Account details `SciuroCard` moved inside `SheetList` content
- `SheetList` uses `Modifier.offset(y = (-24).dp).fillParentMaxHeight()` matching AGENTS.md pattern
- Inner `Column` added `.verticalScroll(rememberScrollState())` for scrolling long timeline content
- Added missing imports (`verticalScroll`, `rememberScrollState`)

### 2. Dashboard — Content Clipping / Missing Transactions

**Root cause:** `DashboardScreen.kt` — the inner `Column` inside `SheetList(fillParentMaxHeight())` had no `verticalScroll`. When banners + transaction list exceeded viewport height, content was clipped and invisible. The filtering data flow (UI → ViewModel → Repository → SQL) was verified to be logically correct — the issue was purely layout.

**Fix:** Added `.verticalScroll(rememberScrollState())` to the inner `Column` inside `SheetList`. Added missing imports.

### 3. Balance Chart — Date Range Inaccuracy

**Root cause:** `DashboardViewModel.kt:169` — `computeBalanceHistory(allTxs)` used all transactions regardless of selected date range. The UI compensated with `takeLast(N)` which was inaccurate for sparse data (e.g., weekends with no transactions).

**Fix:** Changed to `computeBalanceHistory(filteredTxs)` so the balance chart reflects the selected date range.

## What Was Tested

- Compilation of all three modified modules (`feature-wallet`, `feature-dashboard`, `core-ledger`): PASS
- Pre-existing detekt failures in `core-budget` and `core-classifier` are unrelated
- No unit tests exist for the feature modules (NO-SOURCE)

## Files Changed

| File | Change |
|------|--------|
| `feature-wallet/.../AccountDetailScreen.kt` | Restructured root layout, added `verticalScroll` |
| `feature-dashboard/.../DashboardScreen.kt` | Added `verticalScroll` to inner Column |
| `feature-dashboard/.../DashboardViewModel.kt` | Changed `computeBalanceHistory(allTxs)` → `computeBalanceHistory(filteredTxs)` |
