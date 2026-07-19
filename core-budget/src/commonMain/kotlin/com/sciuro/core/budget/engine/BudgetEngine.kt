package com.sciuro.core.budget.engine

import com.sciuro.core.ledger.db.SciuroDatabase
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.audit.util.currentTimeMillis

class BudgetEngine(
    private val database: SciuroDatabase
) {
    suspend fun processBudgets() {
        val allBudgets = database.budgetQueries.selectAllBudgets().executeAsList()
        val allTransactions = database.transactionRecordQueries.selectAllTransactions().executeAsList()
        
        val now = currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val monthAgo = now - thirtyDaysMs
        
        for (budget in allBudgets) {
            // For B7, we calculate "spent this month" as spent in the last 30 days
            val spentThisMonth = allTransactions.filter {
                it.category_id == budget.category_id && 
                it.direction == "OUTFLOW" &&
                it.timestamp >= monthAgo
            }.sumOf { it.amount }
            
            if (kotlin.math.abs(spentThisMonth - budget.current_spent) > 0.01) {
                database.budgetQueries.updateBudgetSpent(
                    current_spent = spentThisMonth,
                    updated_at = now,
                    id = budget.id
                )
            }
        }
    }
}
