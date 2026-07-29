# core-classifier: Edge Case Checklist

**Inspected:** `SciuroIngestionOrchestrator`, `DefaultTransactionBookingUseCase`, `DefaultEngineTriggerUseCase`, `CategoryResolver`, `AccountMatcher`, `ReviewTierDecider`, `RuleLearner`

**Last updated:** Phase 1-4 fixes applied

---

## 1. HAPPY PATH

- [x] **Flow confirmed:** RawEvent arrives → parse → dedup → resolve category → match account → decide review tier → book transaction → fire downstream engines. All components present and wired.

---

## 2. INPUT EDGE CASES

- [x] **2a — Empty `sourcePackageOrAddress`:** **FIXED** — `AccountMatcher` now guards with `isNotBlank()` before querying.
- [x] **2b — Empty-string merchant (not null):** **FIXED** — `CategoryResolver.resolve()` and `ReviewTierDecider.hasLearnedRule()` now return early when `merchant.isBlank()`.
- [x] **2c — Unicode/locale-sensitive `lowercase()`:** **FIXED** — All `lowercase()` calls in `CategoryResolver`, `ReviewTierDecider`, `RuleLearner`, and `AccountMatcher` replaced with locale-independent `forEach { append(it.lowercaseChar()) }` pattern.
- [x] **2d — NULL `source_type` in stranded DB row:** **CONFIRMED SAFE** — Schema has `source_type TEXT NOT NULL`, so null is impossible at the DB level.
- [x] **2e — No idempotency on duplicate event ID:** **FIXED** — `markProcessing` now has `WHERE status = 'PENDING'` SQL guard, and a Kotlin-side status check inside the dedup mutex prevents re-processing.
- [x] **2f — NaN/Infinity amount:** **FIXED** — `book()` validates `draft.amount.isNaN()` and `isInfinite()` before entering the mutex, dead-lettering invalid amounts.
- [x] **2g — SQL wildcards in merchant:** **CONFIRMED SAFE** — Parameterized queries prevent injection.
- [x] **2h — All non-digit `accountOrChannel`:** **CONFIRMED SAFE** — `takeLast(4).filter { isDigit() }` with empty-string guard.

---

## 3. STATE & TIMING EDGE CASES

- [x] **3a — Cancellation leaves event stranded:** **FIXED** — `markProcessing()` moved inside `dedupMutex.withLock`, so cancellation before the lock no longer leaves the event in PROCESSING state.
- [x] **3b — Same-event-ID concurrency:** **FIXED** — Combined with 2e: SQL-level guard `WHERE status = 'PENDING'` plus Kotlin status check inside mutex prevents re-processing.
- [x] **3c — False dedup positives:** **FIXED** — `findLikelyDuplicate` now also matches on `merchant`, reducing false positives for same-amount, same-timestamp transactions to different merchants.
- [ ] **3d — Engine debounce silently skips:** **KNOWN BEHAVIOR** — 15s debounce per engine is by design. Not a bug, but undocumented. Add documentation when time permits.

---

## 4. USER BEHAVIOR EDGE CASES

- [ ] **4a — Rule oscillation on recategorization:** **KNOWN BEHAVIOR** — `upsertMerchantRule` overwrites on the same key. Last-writer-wins is acceptable. Documented.
- [ ] **4b — Stale old-merchant rules on edits:** **KNOWN BEHAVIOR** — Both old and new merchant spellings persist as separate rules. No cleanup mechanism. Documented.
- [x] **4c — Auto-confirm disabled:** **CONFIRMED SAFE** — Everything returns `MANUAL` review tier. Works as designed.
- [x] **4d — Account deletion leaves dangling rules:** **FIXED** — `AccountRepository.deleteAccount()` now clears `merchant_account_rule` entries for the deleted account inside a `database.transaction { }` block. New `deleteMerchantAccountRuleByAccount` query added.

---

## 5. FAILURE & DEGRADATION

- [x] **5a — Parser returns null = permanent dead letter:** **FIXED** — Parser failure no longer dead-letters the event; it stays PENDING. `recoverStrandedEvents` is now a periodic loop (every 60s) that retries PENDING events.
- [x] **5b — Partial state on `bookTransaction` failure:** **CONFIRMED SAFE** — `bookTransaction` already wraps insert + balance update in `database.transaction { }`.
- [x] **5c — Silent engine failures:** **CONFIRMED SAFE** — By design; failures are traced. No user-facing notification.
- [x] **5d — Infinite retry on source connection loss:** **FIXED** — `collectEventsWithRetry` now has `MAX_RECONNECT_ATTEMPTS = 10`. After 10 failed reconnects, the coroutine exits permanently.

---

## 6. UNDOCUMENTED / IMPLICIT BEHAVIOR

- [ ] **6a — `reviewTier` stored as label string:** **KNOWN** — `ReviewTier` labels are custom strings (`"manual"`, `"auto_silent"`, etc.). Stored as-is in SQLite. If labels change, old rows have stale values. No fix applied — would require enum-based storage or migration.
- [x] **6b — `confidenceThreshold` logic duplicated three times:** **FIXED** — Extracted `val isHighConfidence = draft.confidenceScore >= confidenceThreshold` once, reused for `parseStage`, `extractionMethod`, and `auditSource`.
- [x] **6c — `isUntrustedFallback` overrides all tier logic:** **CONFIRMED SAFE** — Security-driven design choice. Forces `UNTRUSTED` for fallback-parsed events regardless of other signals.
- [ ] **6d — `autoConfirmedAt` set for `AUTO_UNDO` transactions:** **KNOWN** — `AUTO_UNDO` transactions get an `autoConfirmedAt` timestamp even though they can be reverted. Name is slightly misleading but low impact.
