# ADR 018: Budgets Drilldown and Main Application Navigation

## Context
With all primary feature modules (`feature-dashboard`, `feature-wallet`, `feature-kanban`, `feature-budgets`) scaffolded, they needed to be assembled into the `app` module using a cohesive navigation structure.

## Decision
1. **Budgets Drilldown (C3)**: Built the `feature-budgets` UI displaying categorized progress bars based on the 30-day budget tracking model.
2. **Koin Initialization**: Created `SciuroApp.kt` in the main `app` module to initialize Koin Dependency Injection. Linked all Core modules (`core-ledger`, `core-budget`, `core-debt`, `core-investment`) and all Feature modules.
3. **Jetpack Navigation**: Updated `MainActivity.kt` to use a Compose `Scaffold` with a `BottomAppBar` integrating Jetpack Navigation (`androidx.navigation:navigation-compose`). The routes defined are `dashboard`, `wallet`, `budgets`, and `kanban`.

## Consequences
- The "Hello World" placeholder is gone. The application now compiles into a fully navigable shell containing all four major user journeys.
- Phase C (Application Assembly) is functionally complete on a structural level.
