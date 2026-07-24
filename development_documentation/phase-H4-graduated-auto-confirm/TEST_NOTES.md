# Phase H4 — Graduated Auto-Confirm + Undo — TEST_NOTES

## Summary
Implemented a 3-tier graduated review-gate replacing the single `isReviewed` boolean. When user enables transaction auto-confirm in Settings > Automation, high-confidence transactions with linked accounts and resolved categories can auto-book silently or with undo. All LLM-basis decisions respect the "Trust validated LLM" opt-in from Phase H3.

## Changes applied

### H4.1 — Migration 11.sqm
- `ALTER TABLE transaction_record ADD COLUMN review_tier TEXT NOT NULL DEFAULT 'MANUAL'`
- `ALTER TABLE transaction_record ADD COLUMN auto_confirmed_at INTEGER`
- `CREATE INDEX idx_tx_review_tier ON transaction_record(review_tier, is_reviewed, timestamp)`
- Seeds `cat_debt_payment` and `cat_transfer` category rows (INSERT OR IGNORE — idempotent)

### H4.2 — TransactionRecord.sq updates
- Added `review_tier`, `auto_confirmed_at` columns to CREATE TABLE + insertTransaction query (16 params now)
- Added `selectRecentlyAutoConfirmed` query (AUTO_UNDO tier, filtered by `created_at > ?`)
- Added `undoAutoConfirm` update (sets review_tier='MANUAL', is_reviewed=0, auto_confirmed_at=NULL)

### H4.3 — ReviewTier model + decider
- `core-audit/.../ReviewTier.kt` — enum: MANUAL, AUTO_SILENT, AUTO_UNDO
- `core-classifier/.../ReviewTierDecider.kt` — pure decider function:
  - Tier 1 AUTO_SILENT: confidence ≥ silentThreshold (default 0.95) AND hasCategory AND hasAccount AND hasLearnedRule(merchant)
  - Tier 2 AUTO_UNDO: confidence ≥ 0.70 AND hasCategory AND hasAccount
  - Tier 3 MANUAL: everything else
  - Guard: if !autoConfirmEnabled → always MANUAL
- `Transaction` model extended with `reviewTier: String = "MANUAL"`, `autoConfirmedAt: Long? = null`

### H4.4 — Orchestrator integration
- Injected `ReviewTierDecider` (14th constructor param)
- Replaced `isReviewed = confidence >= threshold && category != null && account != null` with decider call
- Sets `reviewTier = tier.label` and `autoConfirmedAt = nowAuto` (if not MANUAL)
- Updated trace detail to include `review_tier`

### H4.5 — TransactionRepository
- `insertTransaction` now passes `review_tier` and `auto_confirmed_at`
- New `undoAutoConfirm(transactionId)`: reverses balance delta, sets review_tier=MANUAL, is_reviewed=0
- New `observeRecentlyAutoConfirmed(sinceMs)`: reactive flow for undo card (deferred UI)
- Undo action publishes audit (UPDATE, USER_MANUAL)

### H4.6 — Settings
- `SettingsProvider`: `isTransactionAutoConfirmEnabled()`, `setTransactionAutoConfirmEnabled()`, `getSilentAutoConfirmThreshold()`, `setSilentAutoConfirmThreshold()`
- `EncryptedSettingsProvider`: persisted under `transaction_auto_confirm` (default false), `silent_auto_confirm_threshold` (default 0.95f)
- `LlmParsingConfig.trustValidatedLlm` is wired through `getLlmConfig()` override (from Phase H3)

### H4.7 — Settings UI
- Added two toggles in Settings > Automation card:
  - "Auto-confirm transactions" — master gate for graduated auto-booking
  - "Trust validated LLM results" — enables 0.75 confidence for validated LLM drafts (Phase H3)

### Tier decision matrix

| Confidence | Category | Account | Learned Rule | Extraction | Tier (when enabled) |
|---|---|---|---|---|---|
| ≥ 0.95 | ✓ | ✓ | ✓ | REGEX | AUTO_SILENT |
| ≥ 0.95 | ✓ | ✓ | ✗ | REGEX | AUTO_UNDO |
| ≥ 0.70 | ✓ | ✓ | any | any | AUTO_UNDO |
| ≥ 0.75 | ✓ | ✓ | any | LLM (trusted) | AUTO_UNDO |
| < 0.70 | any | any | any | any | MANUAL |
| any | ✗ | any | any | any | MANUAL |
| any | any | ✗ | any | any | MANUAL |

## Test results
- [PASS] `detekt` — zero issues
- [PASS] `:core-parsing:testDebugUnitTest` — all 47 parser + 16 pipeline/LLM tests pass
- [PASS] `:core-classifier:testDebugUnitTest` — all 7 CategoryResolver tests pass
- [PASS] `:core-ledger:testDebugUnitTest` — NO-SOURCE (no dedicated ledger tests)
- [PASS] All core modules compile cleanly

## Deferred
- **Dashboard Undo card** (H4.8) — the `observeRecentlyAutoConfirmed` flow and `undoAutoConfirm` action are ready in the repository; UI card deferred to Phase H5 (Observability UX)
- **Kanban tier badges** — auto-confirmed transactions now have `review_tier` column available; badge rendering deferred
- **Per-source override** — the decider infrastructure supports it; settings UI deferred
- **Account-leg prompt** — one-time per-package "link to account" prompt deferred
