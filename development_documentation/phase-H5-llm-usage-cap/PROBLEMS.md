# Phase H5 — LLM Usage Cap — PROBLEMS

## Pre-existing test failures (unrelated to this phase)
Confirmed on the base commit via `git stash` + rerun:

- `core-parsing` — `RegexExtractorsTest.extractAmount parses RM amounts` (`expected:<0.0> but was:<null>`).
- `core-classifier` — `DefaultEngineTriggerUseCaseTest` (6 tests) throw `UnsupportedOperationException` (the `dummyDb` object throws for all queries and is passed straight into `TransactionMatchingEngine`/`ObligationDetectionEngine` etc.).

These block a green `allTests` run independently of this phase's changes.

## Known limitation (accepted)
When the daily cap is hit and deterministic parsing also returns no draft (null amount/direction), the event is dead-lettered rather than routed to the Review Inbox. Representing a "raw notification awaiting review" record would require a schema/model change (a partial-review staging record) and is out of scope here.
