# Phase F3 — Rule Learner, CategoryResolver, BudgetLimitSuggester

## Summary

- Added SQL migration `5.sqm` with `merchant_category_rule` and `recipient_rule` tables for persistent rule memory.
- Created `MerchantCategoryRule.sq` with upserts, selects, and deletes for both tables.
- Created `RuleLearner` in `core-classifier` — subscribes to `DomainEventBus`, listens for `TransactionCategorized`/`TransactionRecategorized`, normalizes the merchant key, persists learned associations, and publishes `MerchantRuleLearned`.
- Created `CategoryResolver` in `core-classifier` — replaces the inline `guessCategoryId()` in the orchestrator. Lookup order: (1) learned merchant→category rule, (2) static heuristic, (3) null.
- Created `BudgetLimitSuggester` in `core-budget` — scans 90 days of transaction history per category, computes trimmed mean (drops top/bottom 10%), publishes `BudgetLimitSuggested` event.
- Updated `SciuroIngestionOrchestrator` to use `CategoryResolver` instead of inline `guessCategoryId()` (removed the inline methods).
- Updated `BudgetsScreen` creation sheet with a `SuggestionChip` showing the suggested limit amount when a category is selected.
- Registered `RuleLearner`, `CategoryResolver`, and `BudgetLimitSuggester` in Koin modules.
- Started `RuleLearner` in `SciuroApp.onCreate()` alongside the orchestrator.

## Files changed

| File | Change |
|------|--------|
| `core-ledger/.../db/5.sqm` | NEW: migration for `merchant_category_rule` and `recipient_rule` |
| `core-ledger/.../db/MerchantCategoryRule.sq` | NEW: upsert/select/delete queries for both tables |
| `core-classifier/.../rule/RuleLearner.kt` | NEW: subscribes to event bus, learns merchant→category |
| `core-classifier/.../rule/CategoryResolver.kt` | NEW: rule-first category resolution with static fallback |
| `core-budget/.../engine/BudgetLimitSuggester.kt` | NEW: 90-day trimmed mean per category |
| `core-classifier/.../di/ClassifierModule.kt` | Registered `RuleLearner`, `CategoryResolver`; updated orchestrator wiring |
| `core-budget/.../di/BudgetModule.kt` | Registered `BudgetLimitSuggester` |
| `core-classifier/.../orchestrator/SciuroIngestionOrchestrator.kt` | Uses `CategoryResolver` instead of inline `guessCategoryId()` |
| `core-audit/.../events/DomainEvent.kt` | Added `merchant` param to `TransactionCategorized` and `TransactionRecategorized` |
| `app/.../SciuroApp.kt` | Starts `RuleLearner` in `onCreate()` |
| `feature-budgets/.../ui/BudgetsScreen.kt` | Added `SuggestionChip` showing `BudgetLimitSuggester` suggestion |
| `core-classifier/build.gradle.kts` | Added `commonTest` source set |
