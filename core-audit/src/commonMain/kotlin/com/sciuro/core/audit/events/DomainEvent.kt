package com.sciuro.core.audit.events

sealed interface DomainEvent {
    data class DebtBalanceUpdated(val debtId: String, val newBalance: Double, val method: String) : DomainEvent
    data class DebtFullyPaidOff(val debtId: String) : DomainEvent
    data class ObligationCycleSettled(val obligationId: String, val transactionId: String) : DomainEvent
    data class ObligationCreated(val obligationId: String) : DomainEvent
    data class BudgetThresholdCrossed(val categoryId: String, val percentUsed: Double) : DomainEvent

    data class TransactionCategorized(val transactionId: String, val categoryId: String, val confidence: Double, val source: String, val merchant: String? = null) : DomainEvent
    data class TransactionRecategorized(val transactionId: String, val oldCategoryId: String, val newCategoryId: String, val merchant: String? = null) : DomainEvent
    data class TransferMatched(val transferLinkId: String, val sourceTxId: String, val destTxId: String, val matchMethod: String) : DomainEvent
    data class TransferUnmatchedFlagged(val transactionId: String, val candidateRecipient: String) : DomainEvent
    data class CashCredited(val cashAccountId: String, val amount: Double, val sourceEvent: String) : DomainEvent
    data class CashDebited(val cashAccountId: String, val amount: Double, val sourceEvent: String) : DomainEvent
    data class CashRecounted(val adjustmentId: String, val variance: Double, val adjustmentType: String) : DomainEvent
    data class RecurringObligationProposed(val obligationId: String, val confidence: Double) : DomainEvent
    data class RecurringObligationConfirmed(val obligationId: String) : DomainEvent
    data class ObligationAmountDrifted(val obligationId: String, val oldAmount: Double, val newAmount: Double) : DomainEvent
    data class BnplRiskThresholdCrossed(val activeBnplCount: Int) : DomainEvent
    data class BudgetLimitSuggested(val categoryId: String, val suggestedAmount: Double) : DomainEvent
    data class InvestmentTransactionRecorded(val accountId: String, val action: String, val unitAmount: Double) : DomainEvent
    data class InvestmentPriceRefreshed(val accountId: String, val newPricePerUnit: Double) : DomainEvent
    data class IncomeRecurrencePatternDetected(val incomeStreamId: String, val expectedNextDate: Long, val amount: Double) : DomainEvent
    data class NewFinanceAppDetected(val packageName: String) : DomainEvent
    data class MerchantRuleLearned(val merchant: String, val categoryId: String) : DomainEvent
    data class RecipientRuleLearned(val accountRef: String, val classification: String) : DomainEvent
}
