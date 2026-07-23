package com.sciuro.core.obligations.engine

import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.obligations.repository.ObligationRepository

class ObligationCycleMatcher(
    private val database: SciuroDatabase,
    private val obligationRepository: ObligationRepository
) {
    suspend fun onTransactionBooked(transactionId: String, amount: Double, direction: String, categoryId: String?, merchant: String?, timestamp: Long) {
        if (direction != "OUTFLOW") return

        val active = database.obligationQueries.selectAllActiveObligations().executeAsList()
        val match = active.find {
            (merchant != null && it.name.contains(merchant, ignoreCase = true)) ||
            (it.category_id != null && it.category_id == categoryId &&
             kotlin.math.abs(it.amount - amount) < 2.0)
        } ?: return

        val newDueDate = computeNextDueDate(match.next_due_date, match.frequency)
        obligationRepository.advanceNextDueDate(match.id, newDueDate)
    }

    private fun computeNextDueDate(currentDueDate: Long, frequency: String): Long {
        return when (frequency) {
            "WEEKLY" -> currentDueDate + 7L * 24L * 60L * 60L * 1000L
            "YEARLY" -> currentDueDate + 365L * 24L * 60L * 60L * 1000L
            else -> currentDueDate + 30L * 24L * 60L * 60L * 1000L
        }
    }
}
