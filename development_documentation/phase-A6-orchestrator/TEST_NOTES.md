# Test Notes: Phase A6 - Actor-Critic Triage & Categorization

## Scope
- Built `SciuroIngestionOrchestrator` to stream and process `RawEvents`.
- Wired Koin dependencies across `core-ingestion`, `core-parsing`, and `core-ledger`.
- Added heuristic categorization mapping.

## Results
- `core-classifier` compiles successfully.
- Coroutines Flow efficiently routes events from Notification buffer through LLM/Regex to SQLite insertion.

## Excluded
- Advanced ML/LLM categorization is deferred. Current mapping relies on static keyword heuristic.
