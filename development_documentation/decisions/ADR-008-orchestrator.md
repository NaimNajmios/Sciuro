# ADR 008: Ingestion Orchestrator

## Context
We need a central engine that connects the ingestion buffer (`NotificationSourceAdapter`), the parsing engine (`SciuroParserPipeline`), and the financial ledger (`TransactionRepository`). This engine must process a continuous stream of events concurrently and categorize them autonomously where possible.

## Decision
1. **SciuroIngestionOrchestrator**: Acts as the central traffic controller in `core-classifier`. It consumes a Kotlin `Flow` from the `NotificationSourceAdapter`.
2. **Auto-Categorization**: A naive heuristics engine attempts to map extracted merchants to known ledger categories (e.g., Starbucks -> Dining).
3. **Inbox Routing**: If the transaction lacks a category (unknown merchant) or was parsed by the LLM Fallback (isConfident = false), it is booked to the Ledger with `isReviewed = false`. This guarantees a human loop.
4. **Coroutine Management**: The orchestrator is started and stopped with an explicit `CoroutineScope`, allowing the UI layer (or background service) to control its lifecycle.

## Consequences
- Clean separation of concerns: Ingestion, Parsing, and Ledger have no knowledge of each other, entirely bridged by the Orchestrator.
- Simple testing: The orchestrator can be fed mock `RawEvent` streams and its database mutations verified instantly.
