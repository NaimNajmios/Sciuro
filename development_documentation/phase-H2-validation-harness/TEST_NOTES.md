# Phase H2 — Validation Harness — TEST_NOTES

## Summary
Expanded the fixture corpus with expected output fields (34 → 48 fixtures across 7 providers + new scenarios). Added 16 new test cases: 6 pipeline integration tests (LLM trigger truth table) and 10 LLM fallback parser tests (validation, circuit breaker, debug capture). Discovered and fixed 2 pre-existing circuit breaker bugs that prevented it from ever working in production.

## Changes applied

### H2.2 — Golden Corpus expansion
- `FixtureLibrary.Fixture` data class extended with 5 nullable expected fields: `expectedAmount`, `expectedDirection`, `expectedMerchant`, `expectedCounterpartyAccount`, `expectedConfident`
- All 34 existing fixtures annotated with expected values (backward-compatible — all new fields are nullable with default null)
- Added 14 new fixtures: self-transfer, aggregator forwarding, multi-currency rejection, BNPL, debt payment, subscription pair, large amount, minimal notification, investment purchase
- Added `count` property for fixture count access
- Total: 48 fixtures across 10 providers

### H2.3 — SciuroParserPipelineTest (6 cases)
- Uses deterministic parser with controllable TestRule + ktor-client-mock with ContentNegotiation for LLM simulation
- Tests: confident regex → LLM not called; deterministic null → LLM succeeds; low confidence → LLM overrides; LLM fails → deterministic fallback; no API key → deterministic returned; both fail → null
- Added `kotlinx-coroutines-core` to `core-parsing/commonTest` dependencies

### H2.4 — LlmFallbackParserTest (10 cases)
- Uses ktor-client-mock with ContentNegotiation + engine { addHandler } pattern
- Tests: null API key → null; valid JSON → parsed; API error → null; malformed JSON → null; empty choices → null; breaker opens after 3 failures; breaker returns null when open; manual reset; debug capture populated; cooldown auto-reset (1ms cooldown + 50ms delay)
- All ktor mock clients configured with ContentNegotiation (critical — deserialization fails without it)

### Bugs discovered and fixed during testing
1. **`circuitBrokenUntil` initialized to 0** — `isCircuitBroken()` checks `System.currentTimeMillis() >= circuitBrokenUntil`. Since `circuitBrokenUntil = 0` initially, this condition is always true before the first breaker open, causing `consecutiveFailures = 0` reset on every call. **Fix**: added `if (circuitBrokenUntil == 0L) return false` guard.
2. **`consecutiveFailures = 0` placed before error check** — in `parse()`, the counter was reset at line 121 BEFORE checking `response.error != null`, so every HTTP call (including errors) reset the counter to 0. **Fix**: moved `consecutiveFailures = 0` to after successful `json.decodeFromString<LlmResult>()`, ensuring it only resets on actual success.
3. **`MockEngine` requires `ContentNegotiation` plugin** — Ktor `body<ChatResponse>()` fails without `install(ContentNegotiation) { json(...) }` on the test HTTP client.

## Test results
- [PASS] 48 fixture corpus (all existing + 14 new, with expected field validation)
- [PASS] `SciuroParserPipelineTest` — 6/6 scenarios pass (confident regex, LLM success, LLM override, LLM fallback, no-key, both-null)
- [PASS] `LlmFallbackParserTest` — 10/10 scenarios pass (null key, valid parse, API error, malformed, empty, breaker open, breaker block, manual reset, debug capture, cooldown reset)
- [PASS] All 47 existing parser rule tests still pass
- [PASS] All 7 classifier CategoryResolver tests still pass
- [PASS] All ledger tests still pass
- [PASS] `detekt` — zero issues
- [PASS] All core modules compile cleanly

## Deferred
- SciuroIngestionOrchestratorTest (JVM end-to-end) — requires adding `jvm()` target to `core-classifier` for in-memory SQLDelight
- StagingRecoveryTest — same constraint
- Engine-specific trace point integration tests
