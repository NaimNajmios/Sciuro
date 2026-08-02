# Phase G4: Domain Event Reliability

## Objective
Make the domain event bus durable so that no event is silently lost when subscribers are slow, temporarily absent, or during process death.

## What Changed

### New files in `core-audit`
- `DomainEventEnvelope.kt` - Durable event envelope with ID, sequence, type, schema version, and criticality flag
- `DomainEventStore.kt` - Interface for durable event persistence and delivery state management
- `DomainEventMetrics.kt` - Data class for bus health metrics (pending, dead-letter, retry, lag, drop counts)
- `DomainEventCodec.kt` - Manual serializer/deserializer for all 27 DomainEvent variants
- `InMemoryEventStore.kt` - In-memory implementation for testing
- `DomainEventBusTest.kt` - Tests for durable publish, subscribe, acknowledgment
- `InMemoryEventStoreTest.kt` - Tests for the in-memory event store
- `DomainEventCodecTest.kt` - Tests for serialization roundtrips

### Modified files in `core-audit`
- `DomainEventBus.kt` - Now accepts optional `DomainEventStore`, persists events durably before live emission, exposes `metrics()`, supports `subscribe()` for durable delivery. Buffer increased from 64 to 256 with explicit `DROP_OLDEST` overflow.
- `build.gradle.kts` - Added `kotlinx-serialization-json` dependency

### New files in `core-ledger`
- `DomainEventLog.sq` - SQLDelight table for immutable event log with indexes on sequence, timestamp, type, and criticality
- `DomainEventDelivery.sq` - SQLDelight table for per-subscriber delivery state with claim, acknowledge, failure, and dead-letter queries
- `SqlDelightEventStore.kt` - SQLDelight implementation of `DomainEventStore`
- `15.sqm` - Schema migration adding both tables

### Modified files in `core-ledger`
- `LedgerModule.kt` - Binds `DomainEventStore` and `DomainEventBus` with the store

### Modified files in `feature-settings`
- `DeveloperSettingsViewModel.kt` - Added `eventBusMetrics` state and `loadEventBusMetrics()`
- `DeveloperTabHealth.kt` - Added Domain Event Bus metrics card with color-coded indicators
- `strings.xml` - Added domain event health UI strings
- `SettingsModule.kt` - Updated Koin binding for DeveloperSettingsViewModel

### Modified test files
- `SciuroIngestionOrchestratorTest.kt` - Updated `dummyDb` with new query properties

## Architecture Decisions

### Per-subscriber delivery state
Each subscriber acknowledges events independently via a composite primary key `(event_id, subscriber_id)`. No global `processed` flag.

### Durable log before live emission
`DomainEventBus.publish()` writes to the event store before emitting to the SharedFlow. If the live flow drops events (due to buffer overflow), the durable store still has them.

### Live flow as optimization only
The `MutableSharedFlow(extraBufferCapacity = 256, DROP_OLDEST)` is a latency optimization. The durable store is the source of truth for delivery guarantees.

### Channel rejected
A `Channel<DomainEvent>` would make subscribers compete for events (point-to-point). The current SharedFlow broadcasts to all subscribers.

### Critical events
`DebtFullyPaidOff` and `NetPositionMilestoneReached` are flagged as critical in the event log. They receive the same durable delivery as all other events.

## Test Results

### core-audit tests (18 tests)
- `DomainEventCodecTest`: 8 tests — all event type serialization roundtrips pass
- `InMemoryEventStoreTest`: 11 tests — append, claim, acknowledge, independent subscribers, expired leases, failure recording, dead lettering, metrics, cleanup
- `DomainEventBusTest`: 8 tests — publish persistence, live flow emission, durable subscriber delivery, historical replay, independent subscribers, idempotent subscribe, metrics, shutdown

### core-ledger compilation
- SQLDelight codegen produces `DomainEventLogQueries` and `DomainEventDeliveryQueries`
- `SqlDelightEventStore` compiles successfully against generated queries

### Notes
- `DomainEventBus()` without arguments still works (no store) for backward compatibility with existing tests
- `ON CONFLICT` UPSERT syntax was removed from SQLDelight queries in favor of `INSERT OR IGNORE` + separate UPDATE for compatibility with SQLDelight 2.0.1
- Test method names use camelCase (not backticks) to avoid JUnit4 `ParentRunner` issues with parentheses in method names
