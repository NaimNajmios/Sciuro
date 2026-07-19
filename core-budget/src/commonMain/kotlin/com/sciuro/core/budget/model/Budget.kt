package com.sciuro.core.budget.model

enum class BudgetPeriod {
    WEEKLY, MONTHLY, YEARLY
}

data class Budget(
    val id: String,
    val categoryId: String,
    val allocatedAmount: Double,
    val currentSpent: Double,
    val period: BudgetPeriod
)
