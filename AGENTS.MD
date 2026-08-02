# AGENTS.md

## Build & Test Commands

```bash
# Build (always use these flags)
./gradlew build --console=plain --build-cache --parallel

# Run a single module's unit tests (Android-side)
./gradlew :<module>:testDebugUnitTest --console=plain

# Run a single module's KMP tests (jvmMain)
./gradlew :<module>:jvmTest --console=plain

# Run all tests
./gradlew allTests --console=plain
```

**Gradle output:** Never use `tail` (not available on Windows). Pipe through `2>&1` then `Select-Object -Last 20` instead: `./gradlew assembleDebug 2>&1 | Select-Object -Last 20`. This avoids leaving stale `.txt` files.

## Module Architecture

Three rigid layers, one-way dependency:

```
app → feature-* → core-*
```

- `core-*` = KMP (`commonMain` + `androidMain` + `jvmMain`). Domain engines, persistence, shared logic.
- `feature-*` = Android-only (UI). Compose screens, ViewModels.
- `app` = thin shell. Wires Koin modules, hosts `NavHost`, `Application` subclass. No business logic.
- `core-ui` = shared Compose primitives and theming (a `core-*` module).
- `core-audit` owns the durable `DomainEventStore` interface, `DomainEventBus` (with `metrics()` and `subscribe()` APIs), `DomainEventCodec`, and `DomainEventEnvelope`. `core-ledger` implements `SqlDelightEventStore` backed by `domain_event_log` and `domain_event_delivery` tables (migration 15.sqm). The bus persists events durably before live emission. Per-subscriber acknowledgment ensures no event is silently lost. Cross-feature communication goes through the Domain Event Bus.

New modules: `core-<noun>` for domain/persistence, `feature-<noun>` for UI. Koin modules go in `<Name>Module.kt` inside each module's `di/` package.

## Dependencies

All versions live in `gradle/libs.versions.toml`. **Never** hardcode a version in `build.gradle.kts` — use `libs.<name>` / `libs.plugins.<name>`.

Existing stack: Koin (DI), Ktor (networking), SQLDelight + SQLCipher (persistence), kotlinx.serialization (JSON). Do not introduce alternatives without approval.

Platform-specific drivers (Android SQLDelight driver, Ktor Android engine) go in `androidMain`, not `commonMain`.

## Kotlin & Compose Conventions

- **ViewModel:** Exposes a single `StateFlow<UiState>` where `UiState` is a `sealed interface` (`Loading`, `Empty`, `Success(data)`, `Error(message)`). Domain-specific variants exist. Never expose `MutableStateFlow`. Pattern: `feature-wallet/.../WalletViewModel.kt`.
- **Composables:** Stateless. State lives in ViewModel. Pass data + callbacks down. Don't call Koin from Composables outside `@Preview`. `@Preview` must be static, no DI params, wrapped in `SciuroTheme { ... }`.
- **UI primitives:** Use `core-ui` components only — `SciuroBottomSheet`, `SciuroTextField`, `SciuroDialog`, `HeroPanel`, `SheetList`. Don't create new ones in feature modules.
- **Full-screen swiping pattern:** `Box(fillMaxSize) { LazyColumn { item { HeroPanel(...) } ; item { SheetList(offset(-24.dp).fillParentMaxHeight()) { Column { ... } } } } ; FAB(BottomEnd) }`. Content inside `SheetList` uses `Column` + `forEach` — never nested `LazyColumn`.
- **Multi-statement mutations:** Wrap data-critical operations (insert + balance update, balance reversal + delete) AND their audit write in the same `database.transaction { }` block so the mutation and its audit commit (or roll back) together. Call `database.accountQueries.updateBalance()` directly inside the transaction (with named parameters) rather than through `AccountRepository.updateBalance()`. `AuditRepository.logMutation()` is synchronous and safe to call inside the transaction lambda; if it throws, the whole transaction rolls back — no mutation is ever committed unaudited. Use `transactionWithResult { }` when the mutation must return a value. Event-bus publishes (`suspend`) go after the transaction block.
- **Colors:** Only `SCI`-prefixed tokens from `SciuroTheme` (e.g. `SignalIncome`, `SignalDanger`, `AccountColorGreen`). Never `Color(0xFF...)` inline. No hardcoded colors.
- **Strings:** In `app/src/main/res/values/strings.xml`. Not hardcoded in Composables. Not in a Kotlin `object`.

## Phase Workflow & Docs

Work tracked in `development_documentation/phase-<id>-<slug>/`.

- Every phase folder needs `TEST_NOTES.md` (what was tested, result). Add `PROBLEMS.md` for unexpected behavior. Write entries as you test.
- ADRs use format from `supporting_skills/ADR_PROMPT.md`. Place in relevant phase folder.
- `reference/` has planning docs — link from phase docs, don't copy content.
- `supporting_skills/` has reusable agent prompts (e.g. `COMPOSE_SCREEN_ARCHITECTURE_PROMPT.md`). Load and follow when applicable.

## Commits

- One logical change per commit. Subject ≤ 72 chars, imperative mood.
- Scope prefix when module-specific: `core-transfer: fix self-transfer false positive on partial account match`.
- `build:` for Gradle/config, `docs:` for documentation-only.
- Specific messages. "Fix SelfTransferDetector crash when extracted account number is empty string" — not "Fix bug".

## Pre-Merge Checklist

- `./gradlew allTests` passes (or name the relevant subset).
- `TEST_NOTES.md` updated for current phase.
- Schema changes documented, `PROBLEMS.md` records migration edge cases.
- UI changes: before/after in commit body or phase doc.
- No hardcoded colors, no inline version strings, no `printStackTrace()` / raw `Log.e` in production code.