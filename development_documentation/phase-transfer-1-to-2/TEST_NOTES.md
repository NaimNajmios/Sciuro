# Test Notes: Phase Transfer-1–2 — Deterministic Self-Transfer Detection

## Scope
- Extended `StructuredDraft` with `counterpartyAccountNumber` field.
- Added `extractAccountNumber()` regex and `matchesAccountSuffix()` helper to `RegexExtractors.kt`.
- Updated `CimbParserRule` to extract counterparty account number from notification text.
- **Extraction extended to all 7 parser rules**: Maybank, BSN, TnG, GrabPay, Boost, and ShopeePay now also call `extractAccountNumber()` and pass `counterpartyAccountNumber` in their `StructuredDraft`, with matching confidence boost (+0.1f).
- **Expanded regex coverage**: Added `endingAccountNumberRegex` for English "ending XXXX" and Malay "berakhir XXXX" patterns alongside the existing "A/C XXXX" regex. This handles the common "your account ending 1234" and "akaun anda berakhir 1234" phrasing in bank notifications.
- Restructured `TransferDetectionEngine`:
  - Replaced batch `runDetection()` full-table scan with per-transaction `onTransactionBooked()`.
  - **Tier 1 (Deterministic)**: If extracted counterparty account number matches any own account's `account_number` by suffix, links immediately — no time window needed.
  - **Tier 2 (Heuristic)**: Falls back to amount+time (±2 min) matching only when no account number was extractable. Skips Tier 2 entirely if a non-matching account number was found.
  - First-time heuristic pairs auto-link only if the account pair was previously confirmed (via `account_pair_confirmation` table).
- Wired engine in `SciuroIngestionOrchestrator` after `bookTransaction()`.
- `TransferRepository.linkTransactions()` now auto-records pair confirmation.
- Added `:core-transfer` dependency to `:core-classifier` module.

## Test Coverage Added
- **`RegexExtractorsTest.kt`** (16 tests): Dedicated unit tests for `extractAmount()`, `extractMerchant()`, `extractAccountNumber()` (all regex variants: A/C, Account No, Acc, ending, berakhir, masked, null cases, 3-digit rejection), and `matchesAccountSuffix()` (exact match, suffix, mask chars, mismatch, empty inputs).
- **`TransferDetectionEngineTest.kt`** (10 integration tests using in-memory SQLite via JDBC driver): Tier 1 — suffix match links, no time window needed, masked numbers work, cross-account self-transfer links, amount mismatch prevents leg match; Tier 2 — confirmed pair auto-links, unconfirmed pair silently skipped, outside 2-min window skipped.
- **JVM test infrastructure**: Added `jvm()` targets to `core-audit`, `core-ledger`, and `core-transfer` modules with JDBC SQLite driver for in-memory database testing. Added `jvmMain` actual implementations for `currentTimeMillis()` and `generateUuid()`.
- **`ParserTestCase.expectedCounterpartyAccountNumber`**: New field (nullable, defaults to null — no breakage). When set, `runParserTests()` asserts `result.counterpartyAccountNumber` matches expected value.
- **Updated parser rule tests**: CIMB (all 5 cases) and Maybank (3 cases) now assert expected account numbers from their fixture texts.

## Results
- `:core-transfer`, `:core-classifier`, `:core-parsing` compile and build successfully.
- All 7 parsers now populate `counterpartyAccountNumber`, enabling Tier 1 deterministic matching across the full bank/ewallet coverage.
- Deterministic path no longer depends on amount coincidence or 2-minute window — identity is the signal.
- Manual entry paths (WalletViewModel, DashboardViewModel) pass through Tier 2 only (no draft, no counterparty number).
- `runDetection()` removed — no callers existed in the codebase.

## Excluded
- QR payload decode (Transfer-3, stretch goal).
- Dedicated "suggested transfer" UI for first-time heuristic matches. Transactions remain unreviewed and appear in the existing Review Inbox.
- Account number persistence on `transaction_record` (not needed — engine works reactively from the draft at booking time).
