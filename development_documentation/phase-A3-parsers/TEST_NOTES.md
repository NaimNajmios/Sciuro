# Test Notes: Phase A3 - Parsers

## Scope
- Defined `StructuredDraft` and `ParserRule`.
- Created `ParserTestCase` fixture suite.
- Implemented `CimbParserRule` and verified it against English and BM notification samples.

## Results
- Unit tests (`:core-parsing:testDebugUnitTest`) passed successfully.
- `DeterministicParser` correctly iterates over rules.

- Maybank, BSN, TNG, GrabPay, Boost, and ShopeePay rules are fully implemented.
- Anti-fragile regex handles trailing BM phrases (`pada`, `adalah berjaya`) and English (`was successful`).
- 100% pass rate across the full fixture suite.
