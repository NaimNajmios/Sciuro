package com.sciuro.core.budget.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase

class BudgetEngine(
    private val database: SciuroDatabase,
    private val eventBus: DomainEventBus
) {
    suspend fun processBudgets() {
        val allBudgets = database.budgetQueries.selectAllBudgets().executeAsList()
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        val transferTxIds = database.transferLinkQueries.selectAllTransferLinks().executeAsList()
            .flatMap { listOf(it.outflow_transaction_id, it.inflow_transaction_id) }
            .toSet()

        val now = currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)

        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val monthStartMs = calendar.timeInMillis

        calendar.add(java.util.Calendar.MONTH, 1)
        val monthEndMs = calendar.timeInMillis - 1

        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

        for (budget in allBudgets) {
            val periodStartMs = when (budget.period) {
                "WEEKLY" -> now - 7L * 24 * 60 * 60 * 1000
                "YEARLY" -> now - 365L * 24 * 60 * 60 * 1000
                else -> monthStartMs
            }

            val spentThisPeriod = allTransactions.filter { tx ->
                tx.category_id == budget.category_id &&
                tx.direction == "OUTFLOW" &&
                tx.id !in transferTxIds &&
                tx.timestamp >= periodStartMs
            }.sumOf { it.amount }

            val effectiveAllocation = if (budget.rollover == 1L) {
                val prevPeriodStart = when (budget.period) {
                    "WEEKLY" -> periodStartMs - 7L * 24 * 60 * 60 * 1000
                    "YEARLY" -> periodStartMs - 365L * 24 * 60 * 60 * 1000
                    else -> monthStartMs - 30L * 24 * 60 * 60 * 1000
                }
                val spentPrevPeriod = allTransactions.filter { tx ->
                    tx.category_id == budget.category_id &&
                    tx.direction == "OUTFLOW" &&
                    tx.id !in transferTxIds &&
                    tx.timestamp in prevPeriodStart..<periodStartMs
                }.sumOf { it.amount }
                val unusedPrev = maxOf(0.0, budget.allocated_amount - spentPrevPeriod)
                budget.allocated_amount + unusedPrev
            } else {
                budget.allocated_amount
            }

            if (kotlin.math.abs(spentThisPeriod - budget.current_spent) > 0.01) {
                database.budgetQueries.updateBudgetSpent(
                    current_spent = spentThisPeriod,
                    updated_at = now,
                    id = budget.id
                )
            }

            val threshold = budget.alert_threshold_percent ?: 0.8
            val percentUsed = if (budget.allocated_amount > 0) spentThisPeriod / budget.allocated_amount else 0.0

            if (effectiveAllocation > 0 && percentUsed >= threshold && spentThisPeriod > 0) {
                eventBus.publish(DomainEvent.BudgetThresholdCrossed(
                    categoryId = budget.category_id,
                    percentUsed = percentUsed
                ))
            }
        }
    }
}
