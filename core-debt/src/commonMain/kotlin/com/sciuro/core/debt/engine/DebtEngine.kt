package com.sciuro.core.debt.engine

import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.debt.repository.DebtPaymentLinkRepository
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.events.DomainEvent

class DebtEngine(
    private val database: SciuroDatabase,
    private val debtRepository: DebtRepository,
    private val linkRepository: DebtPaymentLinkRepository,
    private val eventBus: DomainEventBus
) {
    suspend fun processDebtPayments() {
        val allDebts = database.debtQueries.selectAllDebts().executeAsList()
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        val transferTxIds = database.transferLinkQueries.selectAllTransferLinks().executeAsList()
            .flatMap { listOf(it.outflow_transaction_id, it.inflow_transaction_id) }
            .toSet()

        val activeDebts = allDebts.filter { it.status != "PAID_OFF" && it.status != "ARCHIVED" }

        for (debt in activeDebts) {
            val debtDirection = debt.direction?.let { dir ->
                try { DebtDirection.valueOf(dir) } catch (_: Exception) { DebtDirection.I_OWE }
            } ?: DebtDirection.I_OWE
            val expectedDirection = if (debtDirection == DebtDirection.I_OWE) "OUTFLOW" else "INFLOW"

            val matchingTransactions = allTransactions.filter {
                it.direction == expectedDirection &&
                (it.category_id == "cat_debt_payment" || it.merchant?.contains(debt.name, ignoreCase = true) == true) &&
                it.id !in transferTxIds
            }

            for (tx in matchingTransactions) {
                val alreadyLinked = linkRepository.isTransactionLinked(tx.id)
                if (!alreadyLinked) {
                    linkRepository.linkPayment(debt.id, tx.id, tx.amount)
                }
            }

            val calculatedRemaining = linkRepository.recalculateDebtBalance(debt.id, debt.principal_amount)

            if (kotlin.math.abs(calculatedRemaining - debt.remaining_balance) > 0.01) {
                database.debtQueries.updateDebtBalance(
                    calculatedRemaining,
                    com.sciuro.core.audit.util.currentTimeMillis(),
                    debt.id
                )
                eventBus.publish(DomainEvent.DebtBalanceUpdated(debt.id, calculatedRemaining, "AUTO_MATCH"))
                if (calculatedRemaining <= 0.0) {
                    eventBus.publish(DomainEvent.DebtFullyPaidOff(debt.id))
                }
            }
        }
    }
}
