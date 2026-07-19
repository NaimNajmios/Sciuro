# ADR 005: Deterministic Parser Design & Fixtures

## Context
Sciuro needs to extract transaction details (Amount, Merchant, Direction) reliably from app notifications. Since bank notification formats occasionally drift, we need a way to detect breakages instantly before bad data pollutes the Ledger.

## Decision
1. **Fixture-First**: We built `ParserTestCase` in `commonTest` which asserts extraction results against hardcoded English and BM notification strings. No parser rule is considered complete until it passes these test cases.
2. **Deterministic Rules**: We created `ParserRule` which encapsulates regex/string matching. If `matches()` returns true (based on package name), `extract()` is called. 
3. **Graceful Degradation**: The parser returns `null` for fields it can't confidently extract (like Merchant), but tries aggressively to extract the Amount and Direction. If Merchant is null, `isConfident` becomes false, routing the transaction to the Manual Review Inbox later in the pipeline.

## Consequences
- Every new supported bank or e-wallet simply requires a new implementation of `ParserRule` and a corresponding `[Bank]ParserRuleTest`.
- We avoid massive `if-else` blocks in a single parser engine.
