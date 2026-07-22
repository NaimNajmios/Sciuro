# Test Notes: Phase Transfer-1–2 — Deterministic Self-Transfer Detection

## Scope
- Extended `StructuredDraft` with `counterpartyAccountNumber` field.
- Added `extractAccountNumber()` regex and `matchesAccountSuffix()` helper to `RegexExtractors.kt`.
- Updated `CimbParserRule` to extract counterparty account number from notification text.
- Restructured `TransferDetectionEngine`:
  - Replaced batch `runDetection()` full-table scan with per-transaction `onTransactionBooked()`.
  - **Tier 1 (Deterministic)**: If extracted counterparty account number matches any own account's `account_number` by suffix, links immediately — no time window needed.
  - **Tier 2 (Heuristic)**: Falls back to amount+time (±2 min) matching only when no account number was extractable. Skips Tier 2 entirely if a non-matching account number was found.
  - First-time heuristic pairs auto-link only if the account pair was previously confirmed (via `account_pair_confirmation` table).
- Wired engine in `SciuroIngestionOrchestrator` after `bookTransaction()`.
- `TransferRepository.linkTransactions()` now auto-records pair confirmation.
- Added `:core-transfer` dependency to `:core-classifier` module.

## Results
- `:core-transfer`, `:core-classifier`, `:core-parsing` compile and build successfully.
- Deterministic path no longer depends on amount coincidence or 2-minute window — identity is the signal.
- Manual entry paths (WalletViewModel, DashboardViewModel) pass through Tier 2 only (no draft, no counterparty number).
- `runDetection()` removed — no callers existed in the codebase.

## Excluded
- QR payload decode (Transfer-3, stretch goal).
- Dedicated "suggested transfer" UI for first-time heuristic matches. Transactions remain unreviewed and appear in the existing Review Inbox.
- Account number persistence on `transaction_record` (not needed — engine works reactively from the draft at booking time).
