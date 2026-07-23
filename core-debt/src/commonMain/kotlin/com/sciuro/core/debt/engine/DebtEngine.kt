package com.sciuro.core.debt.engine

import com.sciuro.core.debt.model.DebtDirection
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.debt.repository.DebtRepository
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.events.DomainEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers

class DebtEngine(
    private val database: SciuroDatabase,
    private val debtRepository: DebtRepository,
    private val eventBus: DomainEventBus
) {
    /**
     * Checks transactions against known debts. If a transaction matches a debt name or is categorized as 'cat_debt_payment',
     * this engine automatically applies it to the remaining balance of the debt.
     *
     * Respects debt direction: I_OWE debts match OUTFLOW transactions,
     * OWED_TO_ME debts match INFLOW transactions.
     */
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

            val payments = allTransactions.filter { 
                it.direction == expectedDirection && 
                (it.category_id == "cat_debt_payment" || it.merchant?.contains(debt.name, ignoreCase = true) == true) &&
                it.id !in transferTxIds
            }

            val totalPayments = payments.sumOf { it.amount }
            val calculatedRemaining = debt.principal_amount - totalPayments
            
            if (kotlin.math.abs(calculatedRemaining - debt.remaining_balance) > 0.01) {
                database.debtQueries.updateDebtBalance(calculatedRemaining, com.sciuro.core.audit.util.currentTimeMillis(), debt.id)
                eventBus.publish(DomainEvent.DebtBalanceUpdated(debt.id, calculatedRemaining, "AUTO_MATCH"))
                if (calculatedRemaining <= 0.0) {
                    eventBus.publish(DomainEvent.DebtFullyPaidOff(debt.id))
                }
            }
        }
    }
}
