# ADR-022: Deterministic Self-Transfer Detection (Two-Tier)

**Date:** 2026-07-22

**Status:** Accepted

**Supersedes:** ADR-010 (original transfer detection with amount+time heuristic only)

## Context
The original `TransferDetectionEngine` matched inflows to outflows purely on amount (±0.01) and time (±2 minutes). This produced false positives when unrelated transactions of the same amount occurred nearby in time (e.g., two RM12 payments). It also missed legitimate self-transfers that crossed the 2-minute boundary (common with IBG/older interbank rails). The previous ADR-010 acknowledged this was a first pass.

## Decision
1. **Two-tier architecture, tried in order:**
   - **Tier 1 (Deterministic)**: If the notification's extracted `counterpartyAccountNumber` matches any registered own `account.account_number` by suffix (ignoring mask chars like `*`/`X`), this is a self-transfer — link immediately. No amount or time check needed.
   - **Tier 2 (Heuristic)**: Unchanged amount+time matching, but only reached when no account number was extractable. First-time pairs under this tier are NOT auto-linked unless the pair was previously confirmed.
2. **Masked-suffix matching**: `matchesAccountSuffix()` strips non-digits from both extracted and stored strings, compares the last N digits where N = min(both lengths). This handles masked numbers like "...7890" and "****7890".
3. **Per-transaction reactive trigger**: `onTransactionBooked()` replaces `runDetection()` batch scan. Called at booking time from `SciuroIngestionOrchestrator` with the `StructuredDraft` still in scope.
4. **Pair confirmation table**: `account_pair_confirmation` records which account pairs have been human-confirmed. `TransferRepository.linkTransactions()` auto-inserts a confirmation row when a transfer is linked.

## Consequences
- Self-transfers are now identifiable even when the two legs arrive hours apart (no time window in Tier 1).
- False positives from amount+time coincidence are structurally eliminated when the notification includes a counterparty account number.
- Manual setup required: users must enter their account numbers once per account via the Edit Details bottom sheet.
- The old `runDetection()` batch method is removed — it had no callers.
- First-time heuristic matches remain unlinked until the user confirms through the existing Review Inbox flow.

## Subsequent Updates
- **2026-07-22**: Counterparty account number extraction extended from CIMB-only to all 7 parser rules (Maybank, BSN, TnG, GrabPay, Boost, ShopeePay).
- **2026-07-22**: Account number regex expanded with `endingAccountNumberRegex` supporting English "ending XXXX" and Malay "berakhir XXXX" patterns, covering the common "your account ending 1234" / "akaun anda berakhir 1234" phrasing.
- **2026-07-22**: Test infrastructure added: `RegexExtractorsTest.kt` (16 unit tests), `TransferDetectionEngineTest.kt` (10 integration tests with in-memory SQLite via JDBC driver), and `expectedCounterpartyAccountNumber` field on `ParserTestCase` with assertions in `runParserTests()`.
