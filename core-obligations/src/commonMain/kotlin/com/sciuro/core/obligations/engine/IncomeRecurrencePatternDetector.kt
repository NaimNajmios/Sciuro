package com.sciuro.core.obligations.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.ledger.db.SciuroDatabase

data class IncomePattern(
    val amount: Double,
    val frequency: String,
    val nextExpectedDate: Long,
    val merchant: String
)

class IncomeRecurrencePatternDetector(
    private val database: SciuroDatabase,
    private val eventBus: DomainEventBus
) {
    suspend fun detectAndPublish(): IncomePattern? {
        val pattern = detectNextIncome() ?: return null
        eventBus.publish(
            DomainEvent.IncomeRecurrencePatternDetected(
                incomeStreamId = pattern.merchant,
                expectedNextDate = pattern.nextExpectedDate,
                amount = pattern.amount
            )
        )
        return pattern
    }

    fun detectNextIncome(): IncomePattern? {
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        val inflows = allTransactions.filter { it.direction == "INFLOW" && it.merchant != null }
        val byMerchant = inflows.groupBy { it.merchant!! }

        val patterns = mutableListOf<IncomePattern>()

        for ((merchant, txs) in byMerchant) {
            if (txs.size < 2) continue

            val sorted = txs.sortedByDescending { it.timestamp }
            val firstAmount = sorted.first().amount
            val allSimilar = sorted.all { kotlin.math.abs(it.amount - firstAmount) < firstAmount * 0.1 }

            if (!allSimilar) continue

            val sortedAsc = sorted.sortedBy { it.timestamp }
            val intervals = sortedAsc.zipWithNext { a, b -> b.timestamp - a.timestamp }

            val avgInterval = intervals.average().toLong()
            val frequency = when {
                avgInterval < 20L * 24 * 60 * 60 * 1000 -> "WEEKLY"
                avgInterval > 50L * 24 * 60 * 60 * 1000 -> "YEARLY"
                else -> "MONTHLY"
            }

            val mostRecent = sorted.first()
            val nextDue = when (frequency) {
                "WEEKLY" -> mostRecent.timestamp + 7L * 24 * 60 * 60 * 1000
                "YEARLY" -> mostRecent.timestamp + 365L * 24 * 60 * 60 * 1000
                else -> mostRecent.timestamp + 30L * 24 * 60 * 60 * 1000
            }

            patterns.add(IncomePattern(
                amount = firstAmount,
                frequency = frequency,
                nextExpectedDate = nextDue,
                merchant = merchant
            ))
        }

        return patterns.maxByOrNull { it.amount }
    }
}
