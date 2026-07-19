# Test Notes: Phase B2 - Transfer detection

## Scope
- Defined SQLDelight schema `TransferLink`.
- Built `TransferDetectionEngine` to pair matching INFLOW/OUTFLOW transactions within a 2-minute window.
- Integrated `core-transfer` module with Koin DI.

## Results
- `core-transfer` builds and compiles successfully.
- Transaction re-categorization executes correctly upon link detection.

## Excluded
- Fuzzy matching (where transfer fees make the inflow amount slightly less than outflow amount) is excluded. Amounts must match exactly.
- Multi-currency transfers (FX rate fluctuations) are excluded.
