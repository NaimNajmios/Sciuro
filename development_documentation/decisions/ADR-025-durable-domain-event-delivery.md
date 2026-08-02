# ADR-025: Durable Domain Event Delivery

## Status
Accepted

## Context
The current `DomainEventBus` wraps a `MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)` with no replay, no persistence, and no acknowledgment. If more than 64 events queue before a subscriber processes them, events are silently dropped. There is no visibility into drops or delivery gaps.

Key subscribers that depend on events: `NetPositionSubscriber`, `RuleLearner`, `BudgetReconciler`, `UniversalEventSubscriber`, `NotificationSuppressionEngine`, `FinanceAppSuggestionSubscriber`.

## Decision
1. Add a durable event log (`domain_event_log`) and per-subscriber delivery state (`domain_event_delivery`) to the SQLDelight database.
2. Change `DomainEventBus` to persist events before live emission.
3. Add a `subscribe()` API for durable delivery with automatic acknowledgment.
4. Retain the SharedFlow as a live broadcast optimization with explicit `DROP_OLDEST` overflow.
5. Add `metrics()` API for Developer Health visibility.
6. Reject a single `Channel<DomainEvent>` for critical events because it would make subscribers compete.

## Consequences

### Positive
- No event is silently lost on slow subscribers
- Developer Health exposes backlog, dead letters, retries, and lag
- Critical events (`DebtFullyPaidOff`, `NetPositionMilestoneReached`) get durable persistence
- Existing subscribers migrate incrementally without breaking changes

### Negative
- SQLDelight schema migration required (15.sqm)
- Additional database writes on every event publish
- More complex subscriber startup logic
- Per-subscriber delivery state increases database size

## Alternatives Considered
- **Channel.UNLIMITED**: Rejected because it's point-to-point and unbounded memory.
- **Global processed flag**: Rejected because each subscriber must acknowledge independently.
- **Replay=1 on SharedFlow**: Rejected because it only covers the most recent event, not process death.
- **No change**: Rejected because the current system silently loses events.
