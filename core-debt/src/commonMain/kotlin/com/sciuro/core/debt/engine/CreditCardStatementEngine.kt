package com.sciuro.core.debt.engine

import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.debt.model.DebtStatus
import com.sciuro.core.debt.model.DebtType
import com.sciuro.core.ledger.db.SciuroDatabase

class CreditCardStatementEngine(
    private val database: SciuroDatabase
) {
    suspend fun getStatementSummary(debtId: String): StatementSummary? {
        val debt = database.debtQueries.selectAllDebts().executeAsList()
            .find { it.id == debtId } ?: return null

        if (debt.debt_type != DebtType.CREDIT_CARD.name) return null

        val allPayments = database.transactionRecordQueries.selectAllTransactions().executeAsList()
            .filter { tx ->
                tx.direction == "OUTFLOW" &&
                debt.associated_account_id != null &&
                tx.account_id == debt.associated_account_id &&
                tx.merchant?.contains(debt.name, ignoreCase = true) == true
            }

        val now = currentTimeMillis()
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
        val currentStatementPayments = allPayments.filter { it.timestamp >= thirtyDaysAgo }

        val totalPayments = currentStatementPayments.sumOf { it.amount }
        val minPayment = debt.remaining_balance * 0.05

        return StatementSummary(
            debtId = debtId,
            statementBalance = debt.remaining_balance,
            paymentsThisCycle = totalPayments,
            minPaymentDue = minPayment,
            paymentCount = currentStatementPayments.size,
            daysRemaining = ((thirtyDaysAgo + 30L * 24 * 60 * 60 * 1000) - now) / (24 * 60 * 60 * 1000)
        )
    }
}

data class StatementSummary(
    val debtId: String,
    val statementBalance: Double,
    val paymentsThisCycle: Double,
    val minPaymentDue: Double,
    val paymentCount: Int,
    val daysRemaining: Long
)
