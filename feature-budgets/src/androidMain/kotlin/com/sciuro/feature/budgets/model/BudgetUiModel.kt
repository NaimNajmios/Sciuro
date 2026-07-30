package com.sciuro.feature.budgets.model

import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Calendar
import kotlin.math.ceil

enum class BudgetHealth { HEALTHY, APPROACHING, OVER }

data class BudgetUiModel(
    val id: String,
    val categoryName: String,
    val allocatedAmount: Double,
    val currentSpent: Double,
    val alertThresholdPercent: Double? = null,
    val categoryIcon: ImageVector? = null,
    val categoryColor: String? = null,
    val period: String = "MONTHLY",
    val rollover: Boolean = false
) {
    val progress: Float get() = if (allocatedAmount > 0) (currentSpent / allocatedAmount).toFloat() else 0f

    val remaining: Double get() = (allocatedAmount - currentSpent).coerceAtLeast(0.0)

    val daysRemaining: Int get() {
        val cal = Calendar.getInstance()
        return when (period) {
            "WEEKLY" -> {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                7 - dayOfWeek + 1
            }
            "YEARLY" -> {
                val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                366 - dayOfYear
            }
            else -> {
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                maxDay - dayOfMonth + 1
            }
        }
    }

    val dailyAllowance: Double get() {
        val days = daysRemaining
        return if (days > 0) remaining / days else 0.0
    }

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


