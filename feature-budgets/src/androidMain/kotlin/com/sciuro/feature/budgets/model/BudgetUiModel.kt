package com.sciuro.feature.budgets.model

enum class BudgetHealth { HEALTHY, APPROACHING, OVER }

data class BudgetUiModel(
    val id: String,
    val categoryName: String,
    val allocatedAmount: Double,
    val currentSpent: Double,
    val alertThresholdPercent: Double? = null
) {
    val progress: Float get() = if (allocatedAmount > 0) (currentSpent / allocatedAmount).toFloat() else 0f

    fun health(globalThreshold: Float = 0.8f): BudgetHealth {
        if (allocatedAmount <= 0) return BudgetHealth.HEALTHY
        val ratio = currentSpent / allocatedAmount
        val threshold = alertThresholdPercent ?: globalThreshold.toDouble()
        return when {
            ratio >= 1.0 -> BudgetHealth.OVER
            ratio >= threshold -> BudgetHealth.APPROACHING
            else -> BudgetHealth.HEALTHY
        }
    }
}
