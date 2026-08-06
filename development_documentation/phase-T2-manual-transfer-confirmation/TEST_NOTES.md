# Phase T2 — Manual Transfer Confirmation (Kanban "Link as Transfer") — TEST_NOTES

## Summary
Gave users a way to manually confirm an unconfirmed transfer candidate from the Kanban Review tab. The manual path links the two transactions AND records a manual confirmation for the account pair. After 3 manual confirmations of the same canonical account pair, the pair is promoted into `account_pair_confirmation`, enabling future automatic heuristic (Tier 2) and confirmed-pair (Tier 0) matching. Automatic engine links keep their immediate pair-confirmation behavior; they do not count toward the manual threshold.

## Changes applied

### T2.1 — Event now carries the candidate counterpart transaction
- `DomainEvent.TransferUnmatchedFlagged` gained `candidateTransactionId` (the matched opposite-direction leg).
- `DomainEventCodec` serializes/deserializes the new field. Deserialization tolerates legacy persisted events that lack the field (falls back to empty string), so durable event replay of old-format events does not crash.
- `TransferDetectionEngine` publishes `match.id` as `candidateTransactionId`.

### T2.2 — Manual confirmation accounting (schema)
- Migration `16.sqm` creates `account_pair_manual_confirmation(account_id_a, account_id_b, confirmation_count, last_confirmed_at)` keyed by canonical (sorted) pair.
- `Account.sq` declares the table + `selectManualConfirmationCount`, `insertManualConfirmation`, `updateManualConfirmation` queries.
- `AccountRepository.recordManualConfirmation(a, b)` upserts the count inside a transaction and, when the count reaches `MANUAL_CONFIRMATION_THRESHOLD` (3), inserts the pair into `account_pair_confirmation` and writes a `USER_MANUAL` audit entry for the promotion.

### T2.3 — Provenance-aware linking
- `TransferRepository.linkTransactions(link, isManualConfirmation = false)`:
  - `false` (automatic/engine + existing manual transfer-entry path): unchanged — pair confirmed immediately.
  - `true`: pair is NOT confirmed immediately; `recordManualConfirmation` accumulates the count.
- New `TransferRepository.linkCandidatePair(txIdA, txIdB)` resolves outflow/inflow ordering, rejects already-linked pairs, and links with `isManualConfirmation = true`.
- `TransferRepository` now injects `AccountRepository`; `TransferModule` and the engine test updated accordingly.

### T2.4 — Kanban UI
- `KanbanViewModel` stores candidates as `Map<transactionId, candidateTransactionId>`; `transferCandidateIds` remains a derived `Set<String>` so the existing TRANSFER badge still renders.
- New `linkTransferCandidate(transactionId)` calls `transferRepository.linkCandidatePair` and removes the candidate on success; failures surface via the existing error snackbar.
- `KanbanScreen`: "Link as Transfer" outlined button on candidate cards → confirmation dialog (`KanbanDialogs`) → success snackbar "Transfer linked".
- Strings moved to resources (`kanban_transfer_badge`, `kanban_link_as_transfer`, `kanban_link_transfer_title`, `kanban_link_transfer_message`, `kanban_snackbar_transfer_linked`). Hardcoded `"TRANSFER"` badge text replaced.
- `feature-kanban` now depends on `:core-transfer`.

## Test coverage added
- **`TransferDetectionEngineTest`**: Tier 2 heuristic match publishes `TransferUnmatchedFlagged` with the correct `candidateTransactionId`, and the flagged pair stays unlinked.
- **`TransferRepositoryManualLinkTest`** (new, 5 tests):
  - `linkCandidatePair` links + records exactly 1 manual confirmation, without immediately confirming the pair.
  - Inflow/outflow argument order is resolved correctly.
  - Three manual links promote the pair to `account_pair_confirmation`.
  - Already-linked pairs reject and do not increment the count.
  - Automatic `linkTransactions` keeps immediate pair confirmation and does not seed the manual count.
- **`AccountPairConfirmationTest`** (new, 4 tests):
  - Counts accumulate 1 → 2 → 3; pair confirmed only at the threshold.
  - Reversed ordering accumulates onto the same canonical row.
  - Automatic pair insert does not seed the manual count.
  - Threshold promotion writes exactly one `USER_MANUAL` audit entry.
- **`DomainEventCodecTest`**: round-trip for the expanded event + legacy-event tolerance.

## Results
- `:core-transfer:jvmTest` — PASS (all tests including new ones).
- `:core-ledger:jvmTest` — PASS (new `AccountPairConfirmationTest`).
- `:core-audit:jvmTest` — PASS (codec tests).
- `:feature-kanban:assembleDebug` + `:app:compileDebugKotlin` — PASS.
- detekt on touched files/modules: no new issues (pre-existing `SettingsProvider`/`TransactionRepository`/`DomainEventBus`/`KanbanScreen TooManyFunctions` failures are from the in-progress H5 working tree and are out of scope for this phase).

## Notes / edge cases
- Manual confirmations are counted only via the Kanban "Link as Transfer" action. Manually entered transfers (Dashboard) and explicit Settings account-pair linking are unchanged and do not accumulate the manual count.
- The manual-confirmation row is stored under canonical (sorted) ordering; queries must pass IDs in canonical order.
- Legacy `TransferUnmatchedFlagged` events persisted before this phase deserialize with an empty `candidateTransactionId` and are ignored by the Kanban candidate map.
