# Test Notes: Phase B6 - Investment/Gold Savings module

## Scope
- Defined SQLDelight schema `Investment`.
- Implemented `InvestmentRepository` for audited tracking.
- Built `InvestmentEngine` to automatically categorize and accrue asset purchases from standard transactions.
- Created `core-investment` module and Koin DI.

## Results
- Module builds and compiles successfully.
- Cross-module SQLDelight dependencies resolved properly.

## Excluded
- Live price fetching (e.g., Binance/Yahoo Finance API) to calculate real-time Unrealized P&L is excluded.
- Accurate unit calculations from push notifications are excluded due to data limitations. (Failsafe 1:1 RM ratio applied).
