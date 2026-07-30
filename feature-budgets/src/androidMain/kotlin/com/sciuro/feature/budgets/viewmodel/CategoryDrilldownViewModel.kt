package com.sciuro.feature.budgets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sciuro.core.budget.repository.BudgetRepository
import com.sciuro.core.ledger.config.SettingsProvider
import com.sciuro.core.ledger.repository.CategoryRepository
import com.sciuro.core.ledger.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

enum class TimePeriod {
    LAST_30_DAYS,
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS;

    fun cutoffMs(now: Long): Long = when (this) {
        LAST_30_DAYS -> now - 30L * 24 * 60 * 60 * 1000
        THIS_MONTH -> {
            Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        LAST_MONTH -> {
            Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        LAST_3_MONTHS -> now - 90L * 24 * 60 * 60 * 1000
    }

    fun endMs(now: Long): Long? = when (this) {
        LAST_MONTH -> {
            Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        else -> null
    }
}

enum class CategorySpendGroup {
    OVER_BUDGET, APPROACHING_LIMIT, ON_TRACK, NO_BUDGET, UNCATEGORISED
}

data class CategorySpendItem(
    val categoryId: String,
    val name: String,
    val spend: Double,
    val budgetAmount: Double,
    val transactionCount: Int,
    val group: CategorySpendGroup,
    val remaining: Double,
    val daysRemaining: Int,
    val dailyAllowance: Double,
    val budgetPeriod: String?,
    val budgetRollover: Boolean,
    val categoryColor: String?
)

data class CategorySection(
    val group: CategorySpendGroup,
    val items: List<CategorySpendItem>
)

sealed interface CategoryDrilldownState {
    data object Loading : CategoryDrilldownState
    data class Loaded(
        val sections: List<CategorySection>,
        val totalSpend: Double,
        val categoryCount: Int,
        val topCategoryName: String?,
        val sparklineData: List<Float>,
        val periodLabel: String,
        val uncategorisedCount: Int
    ) : CategoryDrilldownState
    data object Empty : CategoryDrilldownState
    data class Error(val message: String) : CategoryDrilldownState
}

class CategoryDrilldownViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val timePeriod = MutableStateFlow(TimePeriod.LAST_30_DAYS)

    val state: StateFlow<CategoryDrilldownState> = combine(
        transactionRepository.observeAllTransactions(),
        categoryRepository.observeCategoriesByType("OUTFLOW"),
        budgetRepository.observeBudgets(),
        timePeriod
    ) { transactions, categories, budgets, period ->
        val now = System.currentTimeMillis()
        val cutoff = period.cutoffMs(now)
        val end = period.endMs(now)
        val threshold = settingsProvider.getBudgetWarningThreshold()

        val recentOutflows = transactions.filter { tx ->
            tx.direction == "OUTFLOW" &&
            tx.timestamp >= cutoff &&
            (end == null || tx.timestamp < end)
        }

        val budgetByCategory = budgets.associateBy { it.category_id }
        val byCategory = recentOutflows.groupBy { it.category_id ?: "uncategorised" }

        val nowCal = Calendar.getInstance()
        val items = categories.mapNotNull { cat ->
            val catTxs = byCategory[cat.id] ?: return@mapNotNull null
            val spend = catTxs.sumOf { it.amount }
            if (spend <= 0.0) return@mapNotNull null

            val budget = budgetByCategory[cat.id]
            val budgetAmt = budget?.allocated_amount ?: 0.0
            val rem = (budgetAmt - spend).coerceAtLeast(0.0)

            val (group, daysRemaining, dailyAllow) = if (budget != null && budgetAmt > 0) {
                val days = computeDaysRemaining(budget.period, nowCal)
                val daily = if (days > 0) rem / days else 0.0
                val g = when {
                    spend >= budgetAmt -> CategorySpendGroup.OVER_BUDGET
                    spend >= threshold * budgetAmt -> CategorySpendGroup.APPROACHING_LIMIT
                    else -> CategorySpendGroup.ON_TRACK
                }
                Triple(g, days, daily)
            } else {
                Triple(CategorySpendGroup.NO_BUDGET, 0, 0.0)
            }

            CategorySpendItem(
                categoryId = cat.id,
                name = cat.name,
                spend = spend,
                budgetAmount = budgetAmt,
                transactionCount = catTxs.size,
                group = group,
                remaining = rem,
                daysRemaining = daysRemaining,
                dailyAllowance = dailyAllow,
                budgetPeriod = budget?.period,
                budgetRollover = budget?.rollover == 1L,
                categoryColor = cat.color
            )
        }

        val uncategorisedTxs = byCategory["uncategorised"] ?: emptyList()
        val uncategorisedItems = if (uncategorisedTxs.isNotEmpty()) {
            val spend = uncategorisedTxs.sumOf { it.amount }
            listOf(
                CategorySpendItem(
                    categoryId = "uncategorised",
                    name = "Uncategorised",
                    spend = spend,
                    budgetAmount = 0.0,
                    transactionCount = uncategorisedTxs.size,
                    group = CategorySpendGroup.UNCATEGORISED,
                    remaining = 0.0,
                    daysRemaining = 0,
                    dailyAllowance = 0.0,
                    budgetPeriod = null,
                    budgetRollover = false,
                    categoryColor = null
                )
            )
        } else emptyList()

        val allItems = (items + uncategorisedItems)
            .sortedByDescending { it.spend }
        if (allItems.isEmpty()) {
            return@combine CategoryDrilldownState.Empty
        }
        val totalSpend = allItems.sumOf { it.spend }

        val sections = buildList {
            val groupOrder = listOf(
                CategorySpendGroup.OVER_BUDGET,
                CategorySpendGroup.APPROACHING_LIMIT,
                CategorySpendGroup.ON_TRACK,
                CategorySpendGroup.NO_BUDGET,
                CategorySpendGroup.UNCATEGORISED
            )
            groupOrder.forEach { g ->
                val groupItems = allItems.filter { it.group == g }
                if (groupItems.isNotEmpty()) {
                    add(CategorySection(group = g, items = groupItems))
                }
            }
        }

        val topCategory = allItems
            .filter { it.group != CategorySpendGroup.UNCATEGORISED }
            .maxByOrNull { it.spend }

        val sparklineData = computeSparklineData(transactions, now)
        val periodLabel = period.name.lowercase().replace("_", " ")

        CategoryDrilldownState.Loaded(
            sections = sections,
            totalSpend = totalSpend,
            categoryCount = items.size,
            topCategoryName = topCategory?.name,
            sparklineData = sparklineData,
            periodLabel = periodLabel,
            uncategorisedCount = uncategorisedTxs.size
        )
    }.catch { e ->
        emit(CategoryDrilldownState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryDrilldownState.Loading
    )

    fun setTimePeriod(period: TimePeriod) {
        timePeriod.value = period
    }

    private fun computeSparklineData(
        transactions: List<com.sciuro.core.ledger.db.Transaction_record>,
        now: Long
    ): List<Float> {
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val dayMs = 24L * 60 * 60 * 1000
        val dayTotals = mutableMapOf<Long, Double>()
        for (i in 6 downTo 0) {
            val dayStart = now - i * dayMs
            val dayKey = dayStart / dayMs
            dayTotals[dayKey] = 0.0
        }
        transactions
            .filter { it.direction == "OUTFLOW" && it.timestamp >= sevenDaysAgo }
            .forEach { tx ->
                val dayKey = tx.timestamp / dayMs
                dayTotals[dayKey] = (dayTotals[dayKey] ?: 0.0) + tx.amount
            }
        return dayTotals.entries.sortedBy { it.key }.map { it.value.toFloat() }
    }

    private fun computeDaysRemaining(period: String, now: Calendar): Int {
        return when (period) {
            "WEEKLY" -> 7 - now.get(Calendar.DAY_OF_WEEK) + 1
            "YEARLY" -> 366 - now.get(Calendar.DAY_OF_YEAR)
            else -> {
                val maxDay = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                maxDay - now.get(Calendar.DAY_OF_MONTH) + 1
            }
        }
    }
}
