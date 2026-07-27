package com.sciuro.core.budget.engine

import com.sciuro.core.audit.events.DomainEvent
import com.sciuro.core.audit.events.DomainEventBus
import com.sciuro.core.audit.util.currentTimeMillis
import com.sciuro.core.ledger.db.SciuroDatabase

class BudgetLimitSuggester(
    private val database: SciuroDatabase,
    private val eventBus: DomainEventBus
) {
    companion object {
        private const val LOOKBACK_DAYS = 90
        private const val TRIM_FRACTION = 0.1
    }

    suspend fun suggestLimit(categoryId: String): Double? {
        val lookbackStart = currentTimeMillis() - (LOOKBACK_DAYS * 24L * 60 * 60 * 1000)
        val amounts = database.budgetQueries.selectTransactionAmountsByCategorySince(
            category_id = categoryId,
            timestamp = lookbackStart
        ).executeAsList().sorted()

        if (amounts.size < 3) return null

        val trimCount = (amounts.size * TRIM_FRACTION).toInt().coerceAtLeast(1)

        if (amounts.size <= 2 * trimCount) return null

        val trimmed = amounts.drop(trimCount).dropLast(trimCount)
        return trimmed.average()
    }

    suspend fun suggestAndPublish(categoryId: String): Double? {
        val suggested = suggestLimit(categoryId) ?: return null
        eventBus.publish(DomainEvent.BudgetLimitSuggested(categoryId, suggested))
        return suggested
    }
}
