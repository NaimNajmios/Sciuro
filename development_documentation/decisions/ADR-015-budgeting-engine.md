# ADR 015: Budgeting Intelligence Layer

## Context
Sciuro is an intelligent financial ledger that helps users manage their spending. A fundamental feature is comparing active spending against user-defined budgets (e.g. RM500 for Food & Dining). This logic needs to be deterministic, heavily audited, and fully isolated within the intelligence layer.

## Decision
1. **Module Creation**: Scaffolded `core-budget` KMP module to encapsulate all budget logic.
2. **Schema**: Added `budget_record` table in `SciuroDatabase`. It strictly maps `category_id` as a foreign key to the `Category` table.
3. **Engine**: Created `BudgetEngine`. When triggered, it calculates the rolling 30-day window (`timestamp >= monthAgo`) and sums all OUTFLOWs assigned to that category. If the mathematical sum drifts from the current budget `current_spent` value, it corrects it automatically.
4. **Simplification (Phase B7)**: Instead of complex calendar-month bounds which require `kotlinx-datetime` and introduce cross-platform edge cases, the engine uses a straightforward 30-day rolling window (`30L * 24 * 60 * 60 * 1000` milliseconds). This provides a continuous "monthly" representation of budget health.

## Consequences
- The architecture is now capable of real-time budget updates whenever a new push notification is parsed.
- We deliberately avoided a heavy datetime library dependency in `core-budget`, preserving fast compilation and low binary overhead in this layer.
- Phase B (Intelligence Layer) is complete. The system can now auto-categorize, link transfers, detect recurring debts, track investments, and maintain rolling budget health.
