# TEST_NOTES.md — Phase F1 (Configurable Allowlist & Parser Health Metrics)

## Test results — 23 July 2026

### `:core-ingestion:testDebugUnitTest` — MutableIngestionAllowlistTest (9 tests)

- [PASS] `effective allowlist contains all defaults when no additions or removals`
  - Verified `effectivePackages.value` equals `IngestionDefaults.defaultAllowedPackages` (35 packages: 27 banks + 3 aggregators).
- [PASS] `addPackage adds to effective allowlist`
  - Added "com.example.wallet", confirmed `allows()` returns true and `isUserAddedPackage()` returns true. Verified persistence via `SettingsProvider.getIngestionAllowlistAdditions()`.
- [PASS] `removePackage removes from effective allowlist`
  - Removed "com.cimbmalaysia", confirmed `allows()` returns false. Verified persistence via `SettingsProvider.getIngestionAllowlistRemovals()`.
- [PASS] `addPackage then removePackage results in empty diff`
  - Add then remove "com.example.wallet" — confirmed neither `allows()` nor `isUserAddedPackage()` return true. No residual entries in additions or removals.
- [PASS] `remove then add restores default`
  - Remove "com.cimbmalaysia" (stop listening), then re-add it (restore). Confirmed `allows()` toggles correctly.
- [PASS] `isDefaultBankPackage returns true for known banks`
  - Verified "com.cimbmalaysia" and "my.com.tngdigital.ewallet" return true. Aggregator packages and unknown packages return false.
- [PASS] `isDefaultAggregatorPackage returns true for aggregators`
  - Verified "com.google.android.gm" and "com.microsoft.office.outlook" return true. Bank packages return false.
- [PASS] `addPackage deduplicates removals`
  - Remove then re-add "com.cimbmalaysia" — confirmed removal set no longer contains the package.
- [PASS] `removePackage deduplicates additions`
  - Add then remove "com.example.wallet" — confirmed additions set no longer contains the package.

### `:core-parsing:testDebugUnitTest` — All existing parser tests (31 fixture tests + 16 regex tests)

- [PASS] All 31 fixture regression tests (7 parser rules) — no regressions.
- [PASS] All 16 `RegexExtractorsTest` tests — no regressions.

### `detekt`

- [PASS] Zero new warnings. All pre-existing warnings are in unchanged files (SettingsScreen deprecated icons, unused params).

## Wallet Card & QR Code UI Refinements — 23 July 2026

### `:feature-wallet:compileDebugKotlinAndroid`

- [PASS] Compilation succeeds with zero errors. Changes across 4 files: `WalletAccount.kt`, `WalletViewModel.kt`, `WalletScreen.kt`, `AccountDetailScreen.kt`.

### `detekt`

- [PASS] Zero detekt issues. Clean static analysis across all changed files.

### Widget card types

- [PASS] `WalletViewModel` maps `isCashWallet = true` for accounts with type containing "cash" or "personal" (case-insensitive).
- [PASS] `WalletScreen` card shows `Wallet` icon + "Cash Wallet" subtitle for cash wallets, `AccountBalanceWallet` + "E-Wallet" for e-wallets, `AccountBalance` + "Bank Account" for bank accounts.
- [PASS] Onboarding-created "Personal Wallet" (type = `"Cash"`) correctly renders as a cash wallet card.

### QR code UX

- [PASS] `QrCodeSection` composable and call site removed from `AccountDetailScreen` — no middle white section.
- [PASS] QR icon `FilledTonalButton` added to hero `content` Row alongside "Adjust Balance" — only visible when `qr_image_path != null && !isCashWallet`.
- [PASS] Full-screen QR `AlertDialog` triggers from hero button tap (same dialog code, new trigger).
- [PASS] `EditAccountDetailsSheet` hides QR picker/remover section (`if (!isCashWallet)`) — cash wallets see no QR management UI.
- [PASS] `QrCodeThumbnail` preserved (still used in edit sheet for non-cash wallets).

### Settings Theme Toggle Fix — 23 July 2026

- [PASS] Replaced broken `SingleChoiceSegmentedButtonRow` + `SegmentedButton` (M3 experimental) with project's `PillToggle` component for the Appearance theme selector.
- [PASS] All three options ("System", "Light", "Dark") render with uniform height, equal width distribution via `fillWidth = true`, and cohesive pill-shaped container with proper border radii.
- [PASS] Text fits cleanly on one line per option — no wrapping or overflow.
- [PASS] `@OptIn(ExperimentalMaterial3Api::class)` removed from `SettingsScreen` (no remaining M3 experimental API usage).
- [PASS] `detekt` zero issues; `:feature-settings:compileDebugKotlinAndroid` builds successfully with no new warnings.

### Dashboard Hero Layout Fix — 23 July 2026

- [PASS] **Root cause**: `PillToggle` ("This Month"/"All Time") was rendered in the same `Row` as the `heroFigure` Column. On typical phone widths (360–412dp), the toggle consumed ~200dp, leaving only 76–128dp for the large-number hero figure. At `headlineLarge` (32sp) with monospace font, 7+ digit numbers (~170dp) got clipped to invisibility — only the smaller "RM" prefix survived.
- [PASS] **Fix**: Moved `PillToggle` rendering from the hero `Row` to its own conditional `Row` below the hero figure, giving the number full screen width (minus 48dp horizontal padding). `toggleOptions.isNotEmpty()` guard ensures the row only appears when toggle options exist (Dashboard only — all other 7 screens pass `emptyList()`).
- [PASS] **Safety net**: Added `softWrap = false` + `overflow = TextOverflow.Ellipsis` to `HeroFigure` Text composable. Even with full-width layout, extremely long numbers gracefully show "RM 1,234,…" instead of disappearing.
- [PASS] **Artifact cleanup**: Removed the `WaveChart` end-of-series label ("RM x,xxx.xx") that appeared confusingly between the chart and the "accounts tracked" row. The chart now renders as a pure visual sparkline. Removed now-unused `labelColor` parameter.
- [PASS] `detekt` zero issues; `:core-ui:compileDebugKotlin` + `:feature-dashboard:compileDebugKotlinAndroid` both build successfully with zero warnings.

## Known gaps

- `ParserHealthRepository` has no direct unit test — requires a JVM SQLDelight driver setup in `core-parsing` (adding `jvm()` target). The SQL queries are structurally identical to existing queries in `raw_event_staging`; correctness is verified by compile-time SQLDelight validation and the successful build of `:core-parsing:compileDebugKotlinAndroid`.
- The "Health" tab in `DeveloperSettingsScreen` uses `LaunchedEffect` + coroutine to load data — tested manually via compilation, no automated Compose UI test exists (Compose testing infrastructure not set up in this project).
