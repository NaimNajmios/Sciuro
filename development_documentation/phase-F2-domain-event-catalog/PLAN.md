# Phase F2 — DomainEvent Catalog Completion

## Summary

Added the 18 event types from the automation cascade plan §3 that were missing from the `DomainEvent` sealed interface. No publishers or consumers were changed — this is a pure type-system addition that unblocks P3 (Rule Learner) and P4 (event publisher wiring).

## Events added

| Event | Key payload | Future emitter (P3/P4) |
|-------|------------|------------------------|
| `TransactionCategorized` | transactionId, categoryId, confidence, source | Orchestrator on transaction booking |
| `TransactionRecategorized` | transactionId, oldCategoryId, newCategoryId | User correction flow (Transaction Detail) |
| `TransferMatched` | transferLinkId, sourceTxId, destTxId, matchMethod | TransferDetectionEngine |
| `TransferUnmatchedFlagged` | transactionId, candidateRecipient | TransferDetectionEngine |
| `CashCredited` | cashAccountId, amount, sourceEvent | CashAdjustmentRepository via orchestrator |
| `CashDebited` | cashAccountId, amount, sourceEvent | CashAdjustmentRepository via orchestrator |
| `CashRecounted` | adjustmentId, variance, adjustmentType | Recount flow |
| `RecurringObligationProposed` | obligationId, confidence | ObligationDetectionEngine (P3 ConfidenceTracker) |
| `RecurringObligationConfirmed` | obligationId | User one-tap confirm / auto-confirm (P4) |
| `ObligationAmountDrifted` | obligationId, oldAmount, newAmount | ObligationCycleMatcher (repeated new amount) |
| `BnplRiskThresholdCrossed` | activeBnplCount | DebtRepository |
| `BudgetLimitSuggested` | categoryId, suggestedAmount | BudgetLimitSuggester (P3) |
| `InvestmentTransactionRecorded` | accountId, action, unitAmount | InvestmentEngine (P4 wire-up) |
| `InvestmentPriceRefreshed` | accountId, newPricePerUnit | InvestmentValuationEngine (P5) |
| `IncomeRecurrencePatternDetected` | incomeStreamId, expectedNextDate, amount | IncomeRecurrencePatternDetector (P4 wire-up) |
| `NewFinanceAppDetected` | packageName | Package-install observer (P4) |
| `MerchantRuleLearned` | merchant, categoryId | RuleLearner subscriber (P3) |
| `RecipientRuleLearned` | accountRef, classification | RuleLearner subscriber (P3) |

## Files changed

| File | Change |
|------|--------|
| `core-audit/.../events/DomainEvent.kt` | Extended from 5 to 23 event types |

## Verification

- `./gradlew compileDebugKotlinAndroid` — clean (full-project build, all 22 modules)
- `./gradlew detekt` — zero new warnings
- Existing `KanbanViewModel`'s `when` expression uses `else -> {}` — silent no-op for new types, no breakage
