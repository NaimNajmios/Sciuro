# ADR 003: Audit Log Core Design

## Context
Every data mutation in Sciuro requires a traceable record (Phase A1 requirement). The audit log needs to be universally applied to prevent manual omission by future feature modules. 

## Decision
1. **Repository-Wrapper Pattern**: Created `AuditableRepository` as a base class. It provides a `withAudit` function that takes a mutation block. It executes the mutation and automatically records the associated `AuditLog`.
2. **UUIDs for IDs**: To stay strictly compatible with KMP without external libraries initially, we generate UUID strings natively in `androidMain` (and other platform modules). 
3. **Common Persistence**: `core-ledger` implements the `SqlDelightAuditRepository`. Since the database instance handles the real data, the audit logs live directly alongside it in `SciuroDatabase`.

## Consequences
- Every domain repository (e.g., `BudgetRepository`, `KanbanRepository`) must extend `AuditableRepository` to write mutations.
- The UI layer is completely abstracted away from the audit logging mechanics.
