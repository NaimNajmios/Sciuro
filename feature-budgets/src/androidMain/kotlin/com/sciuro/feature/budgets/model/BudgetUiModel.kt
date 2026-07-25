package com.sciuro.feature.budgets.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Refresh
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

fun mapCategoryIcon(categoryId: String?): ImageVector? {
    return when (categoryId) {
        "cat_dining", "cat_exp_1" -> Icons.Filled.Restaurant
        "cat_groceries", "cat_exp_6" -> Icons.Filled.LocalGroceryStore
        "cat_transport", "cat_exp_2" -> Icons.Filled.DirectionsCar
        "cat_utilities", "cat_exp_3" -> Icons.Filled.Home
        "cat_exp_4" -> Icons.Filled.ShoppingCart
        "cat_exp_5" -> Icons.Filled.Description
        "cat_exp_7" -> Icons.Filled.LocalHospital
        "cat_exp_8" -> Icons.Filled.School
        "cat_exp_9", "cat_inc_6" -> Icons.Filled.MoreHoriz
        "cat_inc_1" -> Icons.Filled.AccountBalance
        "cat_inc_2" -> Icons.Filled.Computer
        "cat_inc_3" -> Icons.Filled.CardGiftcard
        "cat_inc_4" -> Icons.AutoMirrored.Filled.TrendingUp
        "cat_inc_5" -> Icons.Filled.Refresh
        "cat_transfer" -> Icons.Filled.SwapHoriz
        else -> null
    }
}
