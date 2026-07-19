package com.sciuro.feature.budgets.model

data class BudgetUiModel(
    val id: String,
    val categoryName: String,
    val allocatedAmount: Double,
    val currentSpent: Double
) {
    val progress: Float get() = if (allocatedAmount > 0) (currentSpent / allocatedAmount).toFloat() else 0f
}
