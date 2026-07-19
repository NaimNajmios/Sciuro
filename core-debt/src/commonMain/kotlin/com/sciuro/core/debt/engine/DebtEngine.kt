package com.sciuro.core.debt.engine

import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.debt.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers

class DebtEngine(
    private val database: SciuroDatabase,
    private val debtRepository: DebtRepository
) {
    /**
     * Checks transactions against known debts. If a transaction matches a debt name or is categorized as 'cat_debt_payment',
     * this engine automatically applies it to the remaining balance of the debt.
     */
    suspend fun processDebtPayments() {
        val allDebts = database.debtQueries.selectAllDebts().executeAsList()
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        
        // Very basic matching heuristic for Phase B5
        for (debt in allDebts) {
            val payments = allTransactions.filter { 
                it.direction == "OUTFLOW" && 
                (it.category_id == "cat_debt_payment" || it.merchant?.contains(debt.name, ignoreCase = true) == true)
            }
            
            // In reality, we'd need a link table for DebtPayment to avoid double-applying.
            // For B5, we assume the debt engine runs and we track total payments vs original principal to calculate remaining.
            // Or we just update balance when we see a new one.
            // Since `DebtRepository.applyPayment` just decrements, calling it on historical txs multiple times is bad.
            // Let's assume we do a full recalculation.
            
            val totalPayments = payments.sumOf { it.amount }
            val calculatedRemaining = debt.principal_amount - totalPayments
            
            if (kotlin.math.abs(calculatedRemaining - debt.remaining_balance) > 0.01) {
                // Adjust balance based on total history
                // (Using applyPayment wouldn't work well if we recalculate, so we just directly use update balance)
                database.debtQueries.updateDebtBalance(calculatedRemaining, com.sciuro.core.audit.util.currentTimeMillis(), debt.id)
            }
        }
    }
}
