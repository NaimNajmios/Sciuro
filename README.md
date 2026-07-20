# Sciuro 🐿️

Sciuro is an advanced, privacy-first personal finance and asset management application built with Kotlin Multiplatform (KMP). It is designed with rigorous engineering standards to provide full auditability, intelligent automated tracking, and multi-source financial ingestion.

## Key Features

* **Multi-Source Ingestion:** Automated parsing of financial data using LLM-assisted pipelines and notification listeners.
* **Malaysian Payment Channels:** Deep integration and detection rules for local payment platforms, physical wallets, and e-wallets.
* **Investment & Gold Savings:** Native support for tracking complex assets like gold and long-term investments.
* **Audit-First Architecture:** Every data mutation passes through a unified Audit Log, ensuring complete traceability.
* **Kanban Workflow:** A unified task management and issue tracking system deeply integrated into the development process.
* **Transfer Detection:** Intelligent identification of cross-account transfers to prevent duplicate ledger entries.
* **Guided Onboarding:** Initial setup flow securely initializes system ledgers with physical cash on hand.
* **Interactive UI Triage:** Swipe-to-dismiss capabilities for fast transaction approvals and dynamically updated swipeable wallet interfaces to track cash and investments.

## Project Status

The project is fully functional and has completed **Phase D (Personal Deployment)**. All core modules—including the multi-source ingestion engine, automated budget tracking, Kanban workflow, and UI feature modules—are fully integrated and tested.

## Architecture

Sciuro is built using a strict modular Kotlin Multiplatform structure:
* **Core Modules** (`core-*`): Reusable domain layers and intelligence engines:
  - `:core-ledger`, `:core-audit`: Foundational persistence and traceability.
  - `:core-ingestion`, `:core-parsing`, `:core-llm`: Notification extraction and LLM fallback parsing.
  - `:core-classifier`: The central Orchestrator that triages parsed data.
  - `:core-obligations`, `:core-transfer`, `:core-debt`, `:core-investment`, `:core-budget`: Specialized intelligence engines that track assets, liabilities, recurring expenses, and budget thresholds.
* **Feature Modules** (`feature-*`): User-facing capabilities, such as `:feature-dashboard`, `:feature-wallet`, `:feature-budgets`, and `:feature-kanban`.

**Tech Stack:**
* **Dependency Injection:** [Koin](https://insert-koin.io/)
* **Local Persistence:** [SQLDelight](https://cashapp.github.io/sqldelight/)
* **Static Analysis:** [Detekt](https://detekt.dev/)
* **UI Framework:** Jetpack Compose (Android)

## Development Setup

1. **Requirements:**
   - Android Studio Jellyfish (or newer)
   - JDK 17
   - Kotlin 1.9.x

2. **Building the Project:**
   ```bash
   ./gradlew build
   ```

3. **Running Static Analysis (Detekt):**
   ```bash
   ./gradlew detekt
   ```

## Documentation & Agent Rules

To maintain Sprint-grade engineering discipline, all architectural decisions, bugs, and phase transitions are documented in the `development_documentation/` directory. 
- See `INDEX.md` for a complete list of design docs (ADRs) and progress logs.
- See `AGENTS.md` in the root directory for the strict engineering rules and philosophical tenets that all contributors (including AI agents) must follow.
