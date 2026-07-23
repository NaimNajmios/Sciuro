package com.sciuro.core.audit.events

sealed interface DomainEvent {
    data class DebtBalanceUpdated(val debtId: String, val newBalance: Double, val method: String) : DomainEvent
    data class DebtFullyPaidOff(val debtId: String) : DomainEvent
    data class ObligationCycleSettled(val obligationId: String, val transactionId: String) : DomainEvent
    data class ObligationCreated(val obligationId: String) : DomainEvent
    data class BudgetThresholdCrossed(val categoryId: String, val percentUsed: Double) : DomainEvent
}
