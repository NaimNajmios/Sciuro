# Test Notes: Phase B7 - Budgeting logic

## Scope
- Defined SQLDelight schema `Budget`.
- Created `BudgetRepository` for standard CRUD and auditing.
- Implemented `BudgetEngine` with a rolling 30-day epoch millisecond window for tracking `current_spent`.
- Created `core-budget` module and DI integration.

## Results
- `core-budget` module compiles successfully.
- Dependency isolation confirmed (did not require external kotlinx-datetime).

## Excluded
- Strict calendar-month alignment (e.g., reset strictly at midnight on the 1st of the month) is excluded in favor of a rolling 30-day window.
- Weekly or Yearly aggregation is excluded for this iteration.
