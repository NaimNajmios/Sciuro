# ADR 014: Investment and Asset Tracking

## Context
Standard ledgers treat buying shares as a simple expense, which makes the user's net worth look like it's dropping when they are actually accumulating assets. Sciuro needs a specialized engine to track asset purchases (e.g., Luno, Raiz, ASB) and maintain an inventory of units and their average buy price.

## Decision
1. **Module Creation**: Scaffolded `core-investment`.
2. **Schema**: Created `investment_record` table in the central `SciuroDatabase`. It tracks `asset_symbol`, `units_held`, and `average_buy_price`.
3. **Engine Logic**: `InvestmentEngine` scans historical transactions. When an OUTFLOW is directed to a known investment platform (matching the `assetName`), the engine computes the fiat value of the investment.
4. **Simplification (Phase B6)**: Because push notifications from Luno or Raiz do not contain the actual asset price or unit amount (they only say "RM500 transferred to Luno"), the engine temporarily maps fiat investment as a 1:1 unit ratio (1 Unit = 1 RM). 
5. **Auditing**: All mutations to `investment_record` are passed through the `AuditableRepository`.

## Consequences
- We successfully divorce asset accumulation from standard expense tracking.
- The 1:1 unit mapping is a limitation of the data source (push notifications). To calculate true Unrealized P&L in the future, we would either need the user to manually declare their total unit holdings during the B3 Reconciliation process, or integrate a third-party API to fetch live asset prices.
