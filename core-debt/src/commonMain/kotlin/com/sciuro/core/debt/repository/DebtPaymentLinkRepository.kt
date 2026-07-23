package com.sciuro.core.debt.repository

import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.audit.util.generateUuid
import com.sciuro.core.ledger.db.SciuroDatabase

class DebtPaymentLinkRepository(
    private val database: SciuroDatabase
) {
    suspend fun linkPayment(debtId: String, transactionId: String, amountApplied: Double) {
        database.debtPaymentLinkQueries.insertPaymentLink(
            id = generateUuid(),
            debt_id = debtId,
            transaction_id = transactionId,
            amount_applied = amountApplied,
            applied_at = currentTimeMillis()
        )
    }

    suspend fun isTransactionLinked(transactionId: String): Boolean {
        return database.debtPaymentLinkQueries
            .selectPaymentLinkByTransaction(transactionId)
            .executeAsOneOrNull() != null
    }

    suspend fun sumPaymentsForDebt(debtId: String): Double {
        val result = database.debtPaymentLinkQueries.sumPaymentsByDebt(debtId).executeAsOneOrNull()
        return result?.applied_total ?: 0.0
    }

    suspend fun recalculateDebtBalance(debtId: String, principalAmount: Double): Double {
        val totalPaid = sumPaymentsForDebt(debtId)
        return (principalAmount - totalPaid).coerceAtLeast(0.0)
    }
}
