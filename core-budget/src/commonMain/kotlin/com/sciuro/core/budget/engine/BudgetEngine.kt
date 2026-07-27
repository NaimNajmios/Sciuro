package com.sciuro.core.budget.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.ledger.engine.TransactionMatchingEngine
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class BudgetEngine(
    private val database: SciuroDatabase,
    private val eventBus: DomainEventBus,
    private val matchingEngine: TransactionMatchingEngine
) {
    suspend fun processBudgets() {
        val allBudgets = database.budgetQueries.selectAllBudgets().executeAsList()
        val transferTxIds = matchingEngine.getIneligibleTransactionIds()

        val now = currentTimeMillis()
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(tz)
        val monthStartMs = today.minus(today.dayOfMonth - 1, DateTimeUnit.DAY)
            .atStartOfDayIn(tz).toEpochMilliseconds()

        for (budget in allBudgets) {
            val periodStartMs = when (budget.period) {
                "WEEKLY" -> now - 7L * 24 * 60 * 60 * 1000
                "YEARLY" -> now - 365L * 24 * 60 * 60 * 1000
                else -> monthStartMs
            }

            val spentRow = database.budgetQueries.selectSpendByCategory(
                category_id = budget.category_id,
                timestamp = periodStartMs,
                timestamp_ = now + 1
            ).executeAsOne()
            val spentThisPeriod = spentRow.total_spent ?: 0.0

            val effectiveAllocation = if (budget.rollover == 1L) {
                val prevPeriodStart = when (budget.period) {
                    "WEEKLY" -> periodStartMs - 7L * 24 * 60 * 60 * 1000
                    "YEARLY" -> periodStartMs - 365L * 24 * 60 * 60 * 1000
                    else -> {
                        val prevMonth = today.minus(1, DateTimeUnit.MONTH)
                        prevMonth.minus(prevMonth.dayOfMonth - 1, DateTimeUnit.DAY)
                            .atStartOfDayIn(tz).toEpochMilliseconds()
                    }
                }
                val prevSpentRow = database.budgetQueries.selectSpendByCategory(
                    category_id = budget.category_id,
                    timestamp = prevPeriodStart,
                    timestamp_ = periodStartMs
                ).executeAsOne()
                val spentPrevPeriod = prevSpentRow.total_spent ?: 0.0
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
