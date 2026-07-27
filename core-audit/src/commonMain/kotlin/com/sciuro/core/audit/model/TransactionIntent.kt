package com.sciuro.core.audit.model

sealed class TransactionIntent {
    data class SubscriptionPayment(
        val obligationId: String,
        val obligationName: String,
        val nextDueDate: Long
    ) : TransactionIntent()

    data class DebtPayment(
        val debtId: String,
        val debtName: String,
        val remainingBalance: Double,
        val counterpartyName: String?
    ) : TransactionIntent()

    data class DebtCollection(
        val debtId: String,
        val counterparty: String
    ) : TransactionIntent()

    data class Transfer(
        val sourceAccountId: String,
        val sourceAccountName: String
    ) : TransactionIntent()

    data object Unknown : TransactionIntent()
}
