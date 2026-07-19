# ADR 016: Kanban Board Integration

## Context
Sciuro is evolving from purely a productivity tracker (Sprint) into a unified personal asset and task management system. The Kanban board is the primary UI for managing actionable items. This includes standard tasks (e.g., "Pay Rent") and system-generated tasks (e.g., "Review uncategorized transaction").

## Decision
1. **Module Creation**: Scaffolded `feature-kanban` with Jetpack Compose dependencies.
2. **ViewModel**: Built `KanbanViewModel` using Kotlin StateFlows to maintain a reactive list of `KanbanTask`s.
3. **UI**: Created `KanbanScreen` using Jetpack Compose to display a standard 3-column layout (To Do, In Progress, Done).
4. **Integration**: Configured Koin DI for the view models.

## Consequences
- The task tracking system is fully decoupled from the core financial ledger, allowing it to act as an independent presentation layer.
- The UI is built entirely in Jetpack Compose, enabling dynamic drag-and-drop enhancements in the future.
