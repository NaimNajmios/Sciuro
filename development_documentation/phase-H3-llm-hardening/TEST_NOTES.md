# Phase H3 — LLM Hardening — TEST_NOTES

## Summary
Added semantic validation, content-hash cache, config-driven HTTP timeouts/retries, and validated-confidence flow to the LLM fallback parser. Extended `SettingsProvider` with `isTrustValidatedLlmEnabled()` (default off, honoring ADR-006). When a user enables trust, validated LLM drafts earn confidence 0.75 instead of 0.0 — enabling the upcoming graduated auto-confirm pipeline (Phase H4) to potentially auto-book LLM drafts.

## Changes applied

### H3.1 — Semantic validation
- `validateAmount(llmAmount, rawText, rawTitle): Boolean` — if an RM amount is visible in the notification text (via `RegexExtractors.extractAmount`), the LLM amount must be within 10% tolerance or ±RM 1. Returns true if no visible amount to compare against.
- Merchant blankness check: rejects drafts where LLM returned empty/null merchant.
- Validation verdict recorded in trace (verdict: `validated_confident`, `validated_untrusted`, or `validation_failed`).

### H3.2 — Content-hash cache
- In-memory cache: `Map<String, CacheEntry>` keyed by `"${title}|${text}"`
- 24-hour TTL (`CACHE_TTL_MS = 86400000`)
- Cache check before HTTP call; on hit, returns cached draft with zero latency (traced as `cache_hit`)
- Cache store after successful parse

### H3.3 — Config-driven HTTP timeouts/retries
- Changed `expect fun createHttpClient()` → `expect fun createHttpClient(config: LlmParsingConfig)`
- Android `actual` now uses `config.requestTimeoutMs` and `config.maxRetries` instead of hardcoded `30_000` and `3`
- SettingsProvider override in `EncryptedSettingsProvider` wires `getLlmModelName()` into config

### H3.4 — Trust validated LLM setting
- Added `isTrustValidatedLlmEnabled()`/`setTrustValidatedLlmEnabled()` to `SettingsProvider` interface
- Implemented in `EncryptedSettingsProvider` (key: `trust_validated_llm`, default: `false`)
- Added `trustValidatedLlm: Boolean = false` to `LlmParsingConfig`
- `EncryptedSettingsProvider.getLlmConfig()` now returns config with model name and trust setting

### H3.5 — Validated confidence
- `VALIDATED_CONFIDENCE = 0.75f` constant in companion object
- If `validationPassed && config.trustValidatedLlm` → confidence = 0.75 (trace: `validated_confident`)
- If `validationPassed && !trustValidatedLlm` → confidence = 0.0 (trace: `validated_untrusted`)
- If `!validationPassed` → confidence = 0.0 (trace: `validation_failed`)

## Test results
- [PASS] `LlmFallbackParserTest` — 11/11 scenarios pass (including new `validated LLM draft gets elevated confidence when trust enabled`)
- [PASS] `SciuroParserPipelineTest` — 6/6 scenarios pass
- [PASS] All 47 parser rule tests pass
- [PASS] All classifier + ledger tests pass
- [PASS] `detekt` — zero issues
- [PASS] All core modules compile cleanly

## Design decisions
- Cache TTL is 24 hours — long enough to avoid redundant calls for repeated notifications of the same transaction, short enough to expire stale data
- Cache is per-process in-memory only (not persisted). On app restart, the cache is empty and first-time LLM calls run normally
- Semantic validation tolerance is 10% or ±RM1 — accounts for rounding and minor LLM imprecision while catching hallucinations
- Validated LLM confidence (0.75) is above the DEFAULT_CONFIDENCE_THRESHOLD (0.7) — enabling Phase H4 auto-booking for validated+trusted drafts
- Trust setting defaults to off — explicit user opt-in required to auto-trust LLM output (ADR-006 compliance)
